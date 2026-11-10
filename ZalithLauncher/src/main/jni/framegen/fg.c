// ALL Frame Generation (v3, GPU only). Runs as a desktop-GL client of MobileGlues (see fg_gl.h).
// Per real frame N, only while the adaptive gate says generation helps:
//   copy/blit the finished default framebuffer -> texture N, make a small fixed-size luma copy, GPU block motion
//   search (3 steps, early-out on static cells), present a motion-compensated in-between frame, then put real frame N
//   back so the caller's normal swap presents it. Real frames are shown later than without FG (latency, see handoff).
// Design notes from the research round: fixed pixel budget for the flow (WFG: 180/252 px short side), pause when the
// game is too slow (<20 fps, resume >24), do nothing when the game already fills the display (WinNative "yield").
#include <dlfcn.h>
#include <jni.h>
#include "fg_gl.h"
#include <EGL/egl.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <time.h>
#include "fg.h"
#include "../ctxbridges/egl_loader.h"

#define TAG "ALLFG"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define CELL 4                       // motion cell = 4x4 luma texels
#define REACH 3.5f                   // search reach in luma texels (2 + 1 + 0.5)

static int g_state = -1;             // -1 unknown, 0 off/failed, 1 enabled
// ---- config (env + debug file, see reload_cfg) ----
static int g_budget = 216;           // luma short side in px (fixed pixel budget)
static float g_hz = 60.0f;           // display refresh used by the adaptive gate (env ALL_FG_HZ)
static volatile float g_hz_base;       // display refresh from env ALL_FG_HZ or from the in-game Quick Menu (Unlock FPS changes the active rate while playing)
static int g_gate = 1;               // 1 adaptive, 0 always on (testing only)
static int g_fg = 1;                 // test file fg=0: behave as if FG were off without relaunching (compare in one session)
static int g_gon = 70, g_goff = 85;  // generate while real fps < gon% of Hz; stop when > goff%
static int g_pace = 1;               // 0 none, 1 sleep half period (only mode measured to show generated frames), 2 EGL presentation time (available but generated frames were NOT displayed on TECNO)
static int g_blit = 1;               // 1 glBlitFramebuffer for copy/redraw, 0 shader
static int g_mode = 1;               // 1 extrapolate: real frame N goes out at once, then a half-step forward warp of N (no extra latency, no redraw, single tap, no double images); 0 old interpolation prev..N shown before N
static int g_nn = 1;                 // warp samples the real frame with texelFetch (nearest, clamped) instead of bilinear: less blur, harder edges (test key nn); default 1 after the 60 % pan screenshots
static float g_gmax = 70.0f, g_gpay = 1.30f, g_gret = 0.65f; // gate: max native fps to enter, min displayed-frame gain, min real-fps retention (test keys gmax gpay gret); 70 measured at 45/50/60 % scale (real 38-55 fps): gain +45..52 %, retention 0.78-0.80
static float g_probe_s = 10.0f, g_back_s = 30.0f;            // gate: seconds between re-measure probes while on, back-off after a failed probe (keys gprobe gback); 3/10 s flapped 1.2 switches/2 s, 10/30 s 0.4
static int g_budget_prev = 216, g_warm_w, g_warm_h, g_switches;
static float g_pf = 0.5f;           // extrapolation pacing: sleep this fraction of the real period between the real swap and the generated swap (test key pf)
static float g_th0 = 0.12f, g_th1 = 0.35f; // extrapolation: match cost range where the warp fades out to a plain copy of N (test keys th0/th1)
static int g_ssevery = 120;          // refresh the saved GL state with glGet every N frames (1 = every frame)
static int g_tint, g_prof, g_bench;  // tint: marker on generated frames; prof: glFinish per stage; bench 2: copy only
// ---- GL objects ----
static GLuint g_prog_me, g_prog_mix, g_prog_copy, g_prog_luma, g_prog_warp, g_vao;
static GLint u_w_cur, u_w_mv, u_w_rng, u_w_tint, u_w_th, u_w_nn;
static GLuint g_tex[2], g_tex_me, g_lum[2], g_fbo_me, g_fbo_lum[2], g_fbo_t[2];
static int g_w, g_h, g_lw, g_lh, g_mw, g_mh, g_cur, g_have_prev;
static GLint u_me_prev, u_me_cur, u_me_inv, u_me_thr, u_mix_prev, u_mix_cur, u_mix_mv, u_mix_rng, u_mix_tint, u_copy_tex, u_luma_tex, u_luma_off;
// ---- runtime ----
static int g_active, g_swap_set;
static double g_period_ms = 16.0;    // moving average of the real frame interval
static long long g_last_ns;
static EGLBoolean (*p_presTime)(EGLDisplay, EGLSurface, long long);
// ---- saved GL state (cached, refreshed every g_ssevery frames) ----
typedef struct { GLint prog, vao, drawFbo, readFbo, activeTex, vp[4], tex[3], smp[3]; GLboolean cm[4], blend, depth, stencil, scissor, cull, dither, discard; } SavedState;
static SavedState g_ss;
static int g_ss_age = 1 << 30;
// ---- stats ----
static long long g_t0;
static int g_real, g_gen, g_frames_active, g_gets;
static double g_cpu[8];              // save, copy, luma, motion, mix, swapI, redraw, restore (CPU submit time, no glFinish unless prof)
static double g_gpu_prof[5];         // prof=1 only: copy, luma, motion, mix+swap, redraw (glFinish after each)

static long long now_ns(void) { struct timespec t; clock_gettime(CLOCK_MONOTONIC, &t); return t.tv_sec * 1000000000LL + t.tv_nsec; }

// ---- latency probe (test key lat=1): key press -> first real frame whose centre region visibly changed ----
static int g_lat;
static long long g_swt[512]; static int g_swt_n;
static long long g_swg[512]; static int g_swg_n;
static volatile long long g_in_ns;
static volatile int g_in_pend;
void fg_note_input(void) { if (!g_in_pend) { g_in_ns = now_ns(); g_in_pend = 1; } }

static const char* VS =
"#version 330 core\n"
"out vec2 uv;\n"
"void main(){ vec2 p=vec2(float((gl_VertexID<<1)&2), float(gl_VertexID&2)); uv=p; gl_Position=vec4(p*2.0-1.0,0.0,1.0); }\n";

// Motion search on the fixed-size luma copy. d is in luma texels. 3 steps (2,1,0.5), 8 checkerboard samples of the
// 4x4 cell per candidate; a static cell (tiny cost at zero motion) returns at once.
static const char* FS_ME =
"#version 330 core\n"
"uniform sampler2D uPrev; uniform sampler2D uCur; uniform vec2 uInv; uniform float uThr;\n"
"in vec2 uv; out vec4 o;\n"
"const vec2 O[8]=vec2[8](vec2(-1.5,-1.5),vec2(0.5,-1.5),vec2(-0.5,-0.5),vec2(1.5,-0.5),vec2(-1.5,0.5),vec2(0.5,0.5),vec2(-0.5,1.5),vec2(1.5,1.5));\n"
"float cs[8];\n"
"float cost(vec2 d){ float s=0.0; vec2 b=uv-d*uInv;\n"
"  for(int k=0;k<8;k++){ s+=abs(cs[k]-texture(uPrev,b+O[k]*uInv).r); }\n"
"  return s + length(d)*0.002; }\n"
"void main(){\n"
"  for(int k=0;k<8;k++){ cs[k]=texture(uCur,uv+O[k]*uInv).r; }\n"
"  vec2 c=vec2(0.0); float best=cost(c);\n"
"  if(best>=uThr){\n"
"    for(int s=0;s<3;s++){ float st=(s==0)?2.0:((s==1)?1.0:0.5); vec2 base=c;\n"
"      for(int dy=-1;dy<=1;dy++){ for(int dx=-1;dx<=1;dx++){ if(dx==0&&dy==0) continue;\n"
"        vec2 cand=base+vec2(float(dx),float(dy))*st; float v=cost(cand); if(v<best){ best=v; c=cand; } } } } }\n"
"  o=vec4(c/3.5*0.5+0.5, clamp(best*0.5,0.0,1.0), 1.0);\n"
"}\n";

// Round 8 motion search (reads the current-frame taps again for every candidate), kept only for A/B (test key mev=0).
static const char* FS_ME0 =
"#version 330 core\n"
"uniform sampler2D uPrev; uniform sampler2D uCur; uniform vec2 uInv; uniform float uThr;\n"
"in vec2 uv; out vec4 o;\n"
"float cost(vec2 d){ float s=0.0;\n"
"  for(int j=0;j<4;j++){ for(int i=0;i<4;i++){ if(((i+j)&1)==0){\n"
"    vec2 off=(vec2(float(i),float(j))-1.5)*uInv; s+=abs(texture(uCur,uv+off).r-texture(uPrev,uv+off-d*uInv).r); } } }\n"
"  return s + length(d)*0.002; }\n"
"void main(){\n"
"  vec2 c=vec2(0.0); float best=cost(c);\n"
"  if(best>=uThr){\n"
"    for(int s=0;s<3;s++){ float st=(s==0)?2.0:((s==1)?1.0:0.5); vec2 base=c;\n"
"      for(int dy=-1;dy<=1;dy++){ for(int dx=-1;dx<=1;dx++){ if(dx==0&&dy==0) continue;\n"
"        vec2 cand=base+vec2(float(dx),float(dy))*st; float v=cost(cand); if(v<best){ best=v; c=cand; } } } } }\n"
"  o=vec4(c/3.5*0.5+0.5, clamp(best*0.5,0.0,1.0), 1.0);\n"
"}\n";
static GLuint g_prog_me0; static int g_mev = 1;

static const char* FS_MIX =
"#version 330 core\n"
"uniform sampler2D uPrev; uniform sampler2D uCur; uniform sampler2D uMv; uniform vec2 uRng; uniform int uTint;\n"
"in vec2 uv; out vec4 o;\n"
"void main(){\n"
"  vec4 m=texture(uMv,uv); vec2 mv=(m.rg-0.5)*2.0*uRng;\n"
"  vec3 a=texture(uPrev,uv-mv*0.5).rgb; vec3 b=texture(uCur,uv+mv*0.5).rgb;\n"
"  vec3 pa=texture(uPrev,uv).rgb; vec3 pb=texture(uCur,uv).rgb;\n"
"  float agree=dot(abs(a-b),vec3(1.0/3.0));\n"
"  float w=smoothstep(0.08,0.25,max(m.b,agree*2.0));\n"
"  vec3 c=mix(mix(a,b,0.5),mix(pa,pb,0.5),w);\n"
"  if(uTint==1 && uv.x<0.03 && uv.y<0.05) c=vec3(1.0,0.0,0.0);\n"
"  o=vec4(c,1.0);\n"
"}\n";

// Extrapolation: content at q in the real frame N is moved forward half of the last measured displacement (prev->N).
// One texture tap, no blending of two images (no double images); where the match is poor the warp fades to a copy of N.
static const char* FS_WARP =
"#version 330 core\n"
"uniform sampler2D uCur; uniform sampler2D uMv; uniform vec2 uRng; uniform vec2 uTh; uniform int uTint; uniform int uNN;\n"
"in vec2 uv; out vec4 o;\n"
"void main(){\n"
"  vec4 m=texture(uMv,uv); vec2 mv=(m.rg-0.5)*2.0*uRng;\n"
"  float ok=1.0-smoothstep(uTh.x,uTh.y,m.b);\n"
"  vec2 src=uv-mv*0.5*ok;\n"
"  ivec2 sz=textureSize(uCur,0); ivec2 ip=clamp(ivec2(floor(src*vec2(sz))),ivec2(0),sz-ivec2(1));\n"
"  vec3 c=(uNN==1)?texelFetch(uCur,ip,0).rgb:texture(uCur,src).rgb;\n"
"  if(uTint==1 && uv.x<0.03 && uv.y<0.05) c=vec3(1.0,0.0,0.0);\n"
"  o=vec4(c,1.0);\n"
"}\n";

static const char* FS_LUMA =
"#version 330 core\n"
"uniform sampler2D uTex; uniform vec2 uOff; in vec2 uv; out vec4 o;\n"
"void main(){\n"
"  vec3 c=texture(uTex,uv+vec2(-1.0,-1.0)*uOff).rgb+texture(uTex,uv+vec2(1.0,-1.0)*uOff).rgb+texture(uTex,uv+vec2(-1.0,1.0)*uOff).rgb+texture(uTex,uv+vec2(1.0,1.0)*uOff).rgb;\n"
"  o=vec4(dot(c*0.25,vec3(0.299,0.587,0.114)),0.0,0.0,1.0);\n"
"}\n";

// one bilinear tap at the cell centre (2x2 box) instead of four: test key lv=1
static const char* FS_LUMA1 =
"#version 330 core\n"
"uniform sampler2D uTex; uniform vec2 uOff; in vec2 uv; out vec4 o;\n"
"void main(){ vec3 c=texture(uTex,uv).rgb; o=vec4(dot(c,vec3(0.299,0.587,0.114)),0.0,0.0,1.0); }\n";
static GLuint g_prog_luma1; static int g_lv = 0;

static const char* FS_COPY =
"#version 330 core\n"
"uniform sampler2D uTex; in vec2 uv; out vec4 o;\n"
"void main(){ o=vec4(texture(uTex,uv).rgb,1.0); }\n";

static GLuint compile(GLenum type, const char* src) {
    GLuint s = glCreateShader(type);
    if (!s) { LOGE("glCreateShader failed"); return 0; }
    glShaderSource(s, 1, &src, NULL);
    glCompileShader(s);
    GLint ok = 0; glGetShaderiv(s, GL_COMPILE_STATUS, &ok);
    if (!ok) { char log[512]; glGetShaderInfoLog(s, sizeof log, NULL, log); LOGE("shader compile failed: %s", log); glDeleteShader(s); return 0; }
    return s;
}

static GLuint program(const char* fs) {
    GLuint v = compile(GL_VERTEX_SHADER, VS), f = compile(GL_FRAGMENT_SHADER, fs);
    if (!v || !f) return 0;
    GLuint p = glCreateProgram();
    glAttachShader(p, v); glAttachShader(p, f); glLinkProgram(p);
    glDeleteShader(v); glDeleteShader(f);
    GLint ok = 0; glGetProgramiv(p, GL_LINK_STATUS, &ok);
    if (!ok) { char log[512]; glGetProgramInfoLog(p, sizeof log, NULL, log); LOGE("program link failed: %s", log); glDeleteProgram(p); return 0; }
    return p;
}

static GLuint make_tex(int w, int h, GLint filter) {
    GLuint t; glGenTextures(1, &t); glBindTexture(GL_TEXTURE_2D, t);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    return t;
}

static int fbo_for(GLuint* fbo, GLuint tex) {
    if (!*fbo) glGenFramebuffers(1, fbo);
    glBindFramebuffer(GL_FRAMEBUFFER, *fbo);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
    return glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE;
}

static void release(void) {
    if (g_tex[0]) glDeleteTextures(2, g_tex);
    if (g_tex_me) glDeleteTextures(1, &g_tex_me);
    if (g_lum[0]) glDeleteTextures(2, g_lum);
    g_tex[0] = g_tex[1] = g_tex_me = g_lum[0] = g_lum[1] = 0; g_have_prev = 0;
}

static int resize(int w, int h) {
    release();
    g_w = w; g_h = h;
    int shortSide = w < h ? w : h;
    float k = (float)g_budget / (float)shortSide; if (k > 1.0f) k = 1.0f;   // fixed pixel budget, never upscale
    g_lw = (int)(w * k + 0.5f); g_lh = (int)(h * k + 0.5f);
    if (g_lw < 16) g_lw = 16; if (g_lh < 16) g_lh = 16;
    g_mw = (g_lw + CELL - 1) / CELL; g_mh = (g_lh + CELL - 1) / CELL;
    g_tex[0] = make_tex(w, h, GL_LINEAR); g_tex[1] = make_tex(w, h, GL_LINEAR);
    g_lum[0] = make_tex(g_lw, g_lh, GL_LINEAR); g_lum[1] = make_tex(g_lw, g_lh, GL_LINEAR);
    g_tex_me = make_tex(g_mw, g_mh, GL_LINEAR);
    int ok = fbo_for(&g_fbo_me, g_tex_me) && fbo_for(&g_fbo_lum[0], g_lum[0]) && fbo_for(&g_fbo_lum[1], g_lum[1])
          && fbo_for(&g_fbo_t[0], g_tex[0]) && fbo_for(&g_fbo_t[1], g_tex[1]);
    if (!ok) LOGE("framebuffer incomplete");
    LOGI("resize %dx%d luma %dx%d motion %dx%d", w, h, g_lw, g_lh, g_mw, g_mh);
    return ok;
}

// Debug/test overrides, re-read every stats window so a run can compare settings live. Release use needs no file.
// <app files>/ALL_FG_TEST.txt, e.g. "gate=0 pace=2 blit=1 ssn=120 budget=216 hz=120 gon=70 goff=85 prof=0 bench=0 tint=0"
static void reload_cfg(void) {
#ifdef NDEBUG   // release native build: no test file, no experiment keys (lv, bench, mev, ...) can be switched on
    return;
#else
    // every reload starts from the defaults, so deleting the file (fg_reset.bat) or leaving a key out restores the shipped behaviour
    static float tint0; static int got0;
    if (!got0) { tint0 = (float)g_tint; got0 = 1; }
    g_hz = g_hz_base > 0 ? g_hz_base : g_hz; g_tint = (int)tint0;
    g_gate = 1; g_fg = 1; g_gon = 70; g_goff = 85; g_pace = 1; g_blit = 1; g_mode = 1; g_nn = 1; g_mev = 1; g_lv = 0;
    g_gmax = 70.0f; g_gpay = 1.30f; g_gret = 0.65f; g_probe_s = 10.0f; g_back_s = 30.0f; g_pf = 0.5f; g_th0 = 0.12f; g_th1 = 0.35f;
    g_ssevery = 120; g_budget = 216; g_prof = 0; g_bench = 0; g_lat = 0;
    FILE* cf = fopen("/sdcard/Android/data/com.all.launcher.app.debug.music/files/ALL_FG_TEST.txt", "r");
    if (!cf) { if (g_budget != g_budget_prev) { g_budget_prev = g_budget; g_w = 0; g_warm_w = 0; } return; }
    char buf[160] = {0}; fread(buf, 1, sizeof buf - 1, cf); fclose(cf);
    char* p;
    if ((p = strstr(buf, "gate="))) g_gate = atoi(p + 5);
    if ((p = strstr(buf, "fg="))) g_fg = atoi(p + 3);
    if ((p = strstr(buf, "pace="))) g_pace = atoi(p + 5);
    if ((p = strstr(buf, "blit="))) g_blit = atoi(p + 5);
    if ((p = strstr(buf, "ssn="))) g_ssevery = atoi(p + 4);
    if ((p = strstr(buf, "budget="))) g_budget = atoi(p + 7);
    if ((p = strstr(buf, "hz="))) g_hz = (float)atof(p + 3);
    if ((p = strstr(buf, "gon="))) g_gon = atoi(p + 4);
    if ((p = strstr(buf, "goff="))) g_goff = atoi(p + 5);
    if ((p = strstr(buf, "prof="))) g_prof = atoi(p + 5);
    if ((p = strstr(buf, "bench="))) g_bench = atoi(p + 6);
    if ((p = strstr(buf, "tint="))) g_tint = atoi(p + 5);
    if ((p = strstr(buf, "mode="))) g_mode = atoi(p + 5);
    if ((p = strstr(buf, "lat="))) g_lat = atoi(p + 4);
    if ((p = strstr(buf, "nn="))) g_nn = atoi(p + 3);
    if ((p = strstr(buf, "mev="))) g_mev = atoi(p + 4);
    if ((p = strstr(buf, "lv="))) g_lv = atoi(p + 3);
    if ((p = strstr(buf, "gmax="))) g_gmax = (float)atof(p + 5);
    if ((p = strstr(buf, "gpay="))) g_gpay = (float)atof(p + 5);
    if ((p = strstr(buf, "gret="))) g_gret = (float)atof(p + 5);
    if ((p = strstr(buf, "gprobe="))) g_probe_s = (float)atof(p + 7);
    if ((p = strstr(buf, "gback="))) g_back_s = (float)atof(p + 6);
    if (g_budget != g_budget_prev) { g_budget_prev = g_budget; g_w = 0; g_warm_w = 0; }   // new luma budget: reallocate (and re-warm) on the next frame
    if ((p = strstr(buf, "pf="))) g_pf = (float)atof(p + 3);
    if (g_pf < 0.0f) g_pf = 0.0f; if (g_pf > 1.0f) g_pf = 1.0f;
    if ((p = strstr(buf, "th0="))) g_th0 = (float)atof(p + 4);
    if ((p = strstr(buf, "th1="))) g_th1 = (float)atof(p + 4);
    if (g_th1 <= g_th0) g_th1 = g_th0 + 0.01f;
    if (g_ssevery < 1) g_ssevery = 1;
    if (g_budget < 90) g_budget = 90; if (g_budget > 540) g_budget = 540;

#endif
    if (g_hz < 30.0f) g_hz = 60.0f;
}

static int init(void) {
    if (!fg_gl_load()) { LOGE("cannot load MobileGlues GL entry points (FG works with MobileGlues only)"); return 0; }
    g_prog_me = program(FS_ME); g_prog_mix = program(FS_MIX); g_prog_copy = program(FS_COPY); g_prog_luma = program(FS_LUMA); g_prog_warp = program(FS_WARP);
    g_prog_me0 = program(FS_ME0); g_prog_luma1 = program(FS_LUMA1);
    if (!g_prog_luma1) return 0;
    if (!g_prog_me0 || !g_prog_me || !g_prog_mix || !g_prog_copy || !g_prog_luma || !g_prog_warp) return 0;
    u_w_cur = glGetUniformLocation(g_prog_warp, "uCur"); u_w_mv = glGetUniformLocation(g_prog_warp, "uMv");
    u_w_rng = glGetUniformLocation(g_prog_warp, "uRng"); u_w_th = glGetUniformLocation(g_prog_warp, "uTh"); u_w_tint = glGetUniformLocation(g_prog_warp, "uTint"); u_w_nn = glGetUniformLocation(g_prog_warp, "uNN");
    u_me_prev = glGetUniformLocation(g_prog_me, "uPrev"); u_me_cur = glGetUniformLocation(g_prog_me, "uCur");
    u_me_inv = glGetUniformLocation(g_prog_me, "uInv"); u_me_thr = glGetUniformLocation(g_prog_me, "uThr");
    u_mix_prev = glGetUniformLocation(g_prog_mix, "uPrev"); u_mix_cur = glGetUniformLocation(g_prog_mix, "uCur");
    u_mix_mv = glGetUniformLocation(g_prog_mix, "uMv"); u_mix_rng = glGetUniformLocation(g_prog_mix, "uRng");
    u_mix_tint = glGetUniformLocation(g_prog_mix, "uTint"); u_copy_tex = glGetUniformLocation(g_prog_copy, "uTex");
    u_luma_tex = glGetUniformLocation(g_prog_luma, "uTex"); u_luma_off = glGetUniformLocation(g_prog_luma, "uOff");
    glGenVertexArrays(1, &g_vao);
    const char* hz = getenv("ALL_FG_HZ"); if (hz) g_hz = (float)atof(hz);
    if (g_hz_base > 0) g_hz = g_hz_base; else g_hz_base = g_hz;   // a rate pushed by the Quick Menu before the first frame wins over the launch value
#ifndef NDEBUG
    g_tint = getenv("ALL_FG_TINT") != NULL;
#endif
    reload_cfg();
    // can the EGL layer give frames a presentation time (pacing without sleeping)? log it, use it only if present
    p_presTime = (EGLBoolean (*)(EGLDisplay, EGLSurface, long long))eglGetProcAddress_p("eglPresentationTimeANDROID");
    LOGI("config: hz=%.1f budget=%d gate=%d(%d/%d) pace=%d blit=%d ssn=%d eglPresentationTimeANDROID=%s",
         g_hz, g_budget, g_gate, g_gon, g_goff, g_pace, g_blit, g_ssevery, p_presTime ? "available" : "missing");
    return 1;
}

static void save_state(void) {
    SavedState* s = &g_ss;
    glGetIntegerv(GL_CURRENT_PROGRAM, &s->prog); glGetIntegerv(GL_VERTEX_ARRAY_BINDING, &s->vao);
    glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, &s->drawFbo); glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, &s->readFbo);
    glGetIntegerv(GL_ACTIVE_TEXTURE, &s->activeTex); glGetIntegerv(GL_VIEWPORT, s->vp);
    for (int i = 0; i < 3; i++) {
        glActiveTexture(GL_TEXTURE0 + i);
        glGetIntegerv(GL_TEXTURE_BINDING_2D, &s->tex[i]); glGetIntegerv(GL_SAMPLER_BINDING, &s->smp[i]);
    }
    glGetBooleanv(GL_COLOR_WRITEMASK, s->cm);
    s->blend = glIsEnabled(GL_BLEND); s->depth = glIsEnabled(GL_DEPTH_TEST); s->stencil = glIsEnabled(GL_STENCIL_TEST);
    s->scissor = glIsEnabled(GL_SCISSOR_TEST); s->cull = glIsEnabled(GL_CULL_FACE); s->dither = glIsEnabled(GL_DITHER);
    s->discard = glIsEnabled(GL_RASTERIZER_DISCARD);
    g_gets += 25;
}

static void restore_state(void) {
    SavedState* s = &g_ss;
    glBindFramebuffer(GL_READ_FRAMEBUFFER, s->readFbo); glBindFramebuffer(GL_DRAW_FRAMEBUFFER, s->drawFbo);
    for (int i = 2; i >= 0; i--) { glActiveTexture(GL_TEXTURE0 + i); glBindTexture(GL_TEXTURE_2D, s->tex[i]); glBindSampler(i, s->smp[i]); }
    glActiveTexture(s->activeTex);
    glUseProgram(s->prog); glBindVertexArray(s->vao); glViewport(s->vp[0], s->vp[1], s->vp[2], s->vp[3]);
    glColorMask(s->cm[0], s->cm[1], s->cm[2], s->cm[3]);
    if (s->blend) glEnable(GL_BLEND);
    if (s->depth) glEnable(GL_DEPTH_TEST);
    if (s->stencil) glEnable(GL_STENCIL_TEST);
    if (s->scissor) glEnable(GL_SCISSOR_TEST);
    if (s->cull) glEnable(GL_CULL_FACE);
    if (s->dither) glEnable(GL_DITHER);
    if (s->discard) glEnable(GL_RASTERIZER_DISCARD);
}

static void stats_tick(int force_log) {
    long long t = now_ns();
    if (!g_t0) { g_t0 = t; return; }
    if (!force_log && t - g_t0 < 2000000000LL) return;
    double sec = (t - g_t0) / 1e9;
    double n = g_frames_active ? g_frames_active : 1;
    LOGI("real_fps=%.1f generated_fps=%.1f active=%d%% period_ms=%.1f switches=%d glGet/frame=%.1f hz=%.0f | cpu ms/active frame: save=%.2f copy=%.2f luma=%.2f motion=%.2f mix=%.2f swapI=%.2f redraw=%.2f restore=%.2f%s",
         g_real / sec, g_gen / sec, g_real ? 100 * g_frames_active / g_real : 0, g_period_ms, g_switches, (double)g_gets / (g_real ? g_real : 1), g_hz,
         g_cpu[0] / n, g_cpu[1] / n, g_cpu[2] / n, g_cpu[3] / n, g_cpu[4] / n, g_cpu[5] / n, g_cpu[6] / n, g_cpu[7] / n, g_prof ? " [prof]" : "");
    if (g_prof) LOGI("gpu-ish ms/active frame (glFinish after each): copy=%.2f luma=%.2f motion=%.2f mix+swap=%.2f redraw=%.2f",
                     g_gpu_prof[0] / n, g_gpu_prof[1] / n, g_gpu_prof[2] / n, g_gpu_prof[3] / n, g_gpu_prof[4] / n);
    if (g_lat >= 2) {   // dump swap times in microseconds, 40 per log line
        for (int i = 0; i < g_swt_n; i += 40) {
            char line[1024]; int len = snprintf(line, sizeof line, "SWT");
            for (int j = i; j < g_swt_n && j < i + 40 && len < (int)sizeof line - 24; j++) len += snprintf(line + len, sizeof line - len, " %lld", g_swt[j] / 1000);
            LOGI("%s", line);
        }
        g_swt_n = 0;
        for (int i = 0; i < g_swg_n; i += 40) {   // hook-exit times = just before the caller queues the generated frame
            char line[1024]; int len = snprintf(line, sizeof line, "SWG");
            for (int j = i; j < g_swg_n && j < i + 40 && len < (int)sizeof line - 24; j++) len += snprintf(line + len, sizeof line - len, " %lld", g_swg[j] / 1000);
            LOGI("%s", line);
        }
        g_swg_n = 0;
    }
    memset(g_cpu, 0, sizeof g_cpu); memset(g_gpu_prof, 0, sizeof g_gpu_prof);
    g_t0 = t; g_real = g_gen = g_frames_active = g_gets = g_switches = 0;
    reload_cfg();
}

static void draw_full(void) { glDrawArrays(GL_TRIANGLES, 0, 3); }
static void bind_tex(int unit, GLuint t) { glActiveTexture(GL_TEXTURE0 + unit); glBindSampler(unit, 0); glBindTexture(GL_TEXTURE_2D, t); }
static void blit_full(GLuint srcFbo, GLuint dstFbo, int w, int h) {
    glBindFramebuffer(GL_READ_FRAMEBUFFER, srcFbo); glBindFramebuffer(GL_DRAW_FRAMEBUFFER, dstFbo);
    glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);
}

// Adaptive gate: generate only while the game's own frame rate is below the display rate and high enough to help
// (WFG: pause < 20 fps). Entering needs 90 steady frames below the threshold (a hitch must not switch FG on). While
// active the measured period includes FG's own cost, so every 3 s FG pauses for 12 frames to re-measure the native
// rate; it stays on only if the native rate is still below the display rate AND the displayed rate with FG
// (min(Hz, 2*fps_with_fg)) beats the native one by 15%; otherwise it backs off for 10 s. Without that benefit check a
// compositor throttle (idle touch => ~30 Hz presentation) can leave FG on while it halves the real frame rate.
// Decisions use true averages (frame intervals summed over the active window and over a 24-frame probe whose first 3
// frames are dropped), not the short moving average, and a single failed probe only schedules a retry in 3 s: two
// misses in a row are needed before the 30 s back-off (a camera pan changes the load by +-20 % within seconds).
static int g_below, g_probe_left, g_probe_n, g_act_n, g_fail_streak, g_settle;
static long long g_since_ns, g_backoff_ns;
static double g_act_fps, g_probe_dt, g_act_dt, g_last_dt;
static double minf(double a, double b) { return a < b ? a : b; }
static void gate_reset_act(void) { g_act_dt = 0; g_act_n = 0; g_settle = 6; }
static void update_gate(long long t) {
    double fps = 1000.0 / g_period_ms;
    if (!g_gate) { g_active = 1; return; }
    if (g_probe_left > 0) {
        g_active = 0;
        if (g_last_dt > 0 && g_probe_left <= 21) { g_probe_dt += g_last_dt; g_probe_n++; }
        if (--g_probe_left == 0) {
            double nat = g_probe_n >= 10 ? 1000.0 * g_probe_n / g_probe_dt : fps;
            int low = nat < g_hz * g_goff / 100.0 && nat >= 15.0 && nat < g_gmax;
            // worth it only if it adds >= gpay x displayed frames AND the game keeps >= gret of its real fps
            int pays = minf(g_hz, 2.0 * g_act_fps) >= g_gpay * minf(g_hz, nat) && g_act_fps >= g_gret * nat;
            if (low && pays) { g_active = 1; g_fail_streak = 0; g_since_ns = t; gate_reset_act(); }
            else if (low && ++g_fail_streak < 2) { g_active = 1; g_since_ns = t - (long long)((g_probe_s - 3.0f) * 1e9f); gate_reset_act(); }
            else { g_active = 0; g_since_ns = t; if (low) { g_backoff_ns = t + (long long)(g_back_s * 1e9f); g_fail_streak = 0; g_below = 0; } }
        }
        return;
    }
    if (!g_active) {
        if (t < g_backoff_ns) return;
        if (fps < g_hz * g_gon / 100.0 && fps >= 15.0 && fps < g_gmax) { if (++g_below >= 90) { g_active = 1; g_fail_streak = 0; g_since_ns = t; gate_reset_act(); } }
        else g_below = 0;
    } else {
        if (g_settle > 0) g_settle--; else if (g_last_dt > 0) { g_act_dt += g_last_dt; g_act_n++; }
        if (fps < 12.0) { g_active = 0; g_below = 0; g_backoff_ns = t + (long long)(g_back_s * 1e9f); }
        else if (t - g_since_ns >= (long long)(g_probe_s * 1e9f)) {
            g_act_fps = g_act_n >= 60 ? 1000.0 * g_act_n / g_act_dt : fps;
            g_probe_left = 24; g_probe_dt = 0; g_probe_n = 0; g_active = 0;
        }
    }
}

// Reads a 128x64 region at the centre of the finished real frame (glReadPixels waits for the GPU, so the time is "frame
// complete", the same point for FG Off and On) and logs key->frame time when that region changed after a key press.
static void lat_probe(EGLDisplay dpy, EGLSurface surface) {
    EGLint w = 0, h = 0;
    if (!eglQuerySurface_p(dpy, surface, EGL_WIDTH, &w) || !eglQuerySurface_p(dpy, surface, EGL_HEIGHT, &h) || w < 256 || h < 128) return;
    enum { RW = 128, RH = 64 };
    // async: 3 pixel-pack buffers in a ring; this frame's region is requested now, the one requested 2 frames ago is mapped
    // (finished by then, no stall) and judged with the timestamp it was requested at
    static GLuint pbo[3]; static long long pts[3]; static int pi, filled;
    static unsigned char prev[RW * RH * 4]; static int have;
    GLint rfbo = 0, rpbo = 0; glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, &rfbo); glGetIntegerv(GL_PIXEL_PACK_BUFFER_BINDING, &rpbo);
    if (!pbo[0]) {
        glGenBuffers(3, pbo);
        for (int i = 0; i < 3; i++) { glBindBuffer(GL_PIXEL_PACK_BUFFER, pbo[i]); glBufferData(GL_PIXEL_PACK_BUFFER, RW * RH * 4, NULL, GL_STREAM_READ); }
    }
    glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
    glBindBuffer(GL_PIXEL_PACK_BUFFER, pbo[pi]);
    glReadPixels(w / 2 - RW / 2, h / 2 - RH / 2, RW, RH, GL_RGBA, GL_UNSIGNED_BYTE, 0);
    pts[pi] = now_ns();
    int old = (pi + 1) % 3;           // requested 2 frames ago
    pi = (pi + 1) % 3;
    if (filled < 3) filled++;
    unsigned char* cur = NULL; long long tn = 0;
    if (filled >= 3) {
        glBindBuffer(GL_PIXEL_PACK_BUFFER, pbo[old]);
        cur = (unsigned char*)glMapBufferRange(GL_PIXEL_PACK_BUFFER, 0, RW * RH * 4, GL_MAP_READ_BIT);
        tn = pts[old];
    }
    glBindBuffer(GL_PIXEL_PACK_BUFFER, rpbo);
    glBindFramebuffer(GL_READ_FRAMEBUFFER, rfbo);
    if (!cur) return;
    if (have) {
        long diff = 0;
        for (int i = 0; i < RW * RH * 4; i += 4) {
            int dr = cur[i] - prev[i], dg = cur[i + 1] - prev[i + 1], db = cur[i + 2] - prev[i + 2];
            diff += (dr < 0 ? -dr : dr) + (dg < 0 ? -dg : dg) + (db < 0 ? -db : db);
        }
        double avg = diff / (double)(RW * RH * 3);
        if (avg > 12.0) {
            if (g_in_pend) { LOGI("LAT key_to_frame_ms=%.1f change=%.0f period_ms=%.1f fg_active=%d", (tn - g_in_ns) / 1e6, avg, g_period_ms, g_active); g_in_pend = 0; }
        } else if (g_in_pend && tn - g_in_ns > 2000000000LL) { LOGI("LAT timeout (no visible change after key)"); g_in_pend = 0; }
    }
    memcpy(prev, cur, RW * RH * 4); have = 1;
    glBindBuffer(GL_PIXEL_PACK_BUFFER, pbo[old]); glUnmapBuffer(GL_PIXEL_PACK_BUFFER); glBindBuffer(GL_PIXEL_PACK_BUFFER, rpbo);
}

// One-time warm-up: allocate textures/FBOs for the current surface size and run every pass once (blit, luma, motion,
// warp, mix, copy) into FBO textures only, never into the window, so shader first-use compilation and resource creation
// do not land on the first generated frame (measured 0.25-0.6 s stalls).
static void warm(EGLDisplay dpy, EGLSurface surface) {
    EGLint w = 0, h = 0;
    if (!eglQuerySurface_p(dpy, surface, EGL_WIDTH, &w) || !eglQuerySurface_p(dpy, surface, EGL_HEIGHT, &h) || w < 64 || h < 64) return;
    while (glGetError() != GL_NO_ERROR) {}
    long long t0 = now_ns();
    save_state(); g_ss_age = 0;
    if (resize(w, h)) {
        glDisable(GL_BLEND); glDisable(GL_DEPTH_TEST); glDisable(GL_STENCIL_TEST); glDisable(GL_SCISSOR_TEST);
        glDisable(GL_CULL_FACE); glDisable(GL_DITHER); glDisable(GL_RASTERIZER_DISCARD);
        glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
        glBindVertexArray(g_vao);
        blit_full(0, g_fbo_t[0], w, h);
        glBindFramebuffer(GL_FRAMEBUFFER, g_fbo_lum[0]); glViewport(0, 0, g_lw, g_lh);
        glUseProgram(g_prog_luma); bind_tex(0, g_tex[0]); glUniform1i(u_luma_tex, 0); glUniform2f(u_luma_off, 0.25f / g_lw, 0.25f / g_lh);
        draw_full();
        glBindFramebuffer(GL_FRAMEBUFFER, g_fbo_lum[1]); draw_full();
        glBindFramebuffer(GL_FRAMEBUFFER, g_fbo_me); glViewport(0, 0, g_mw, g_mh);
        glUseProgram(g_prog_me); bind_tex(0, g_lum[0]); bind_tex(1, g_lum[1]);
        glUniform1i(u_me_prev, 0); glUniform1i(u_me_cur, 1); glUniform2f(u_me_inv, 1.0f / g_lw, 1.0f / g_lh); glUniform1f(u_me_thr, 0.03f);
        draw_full();
        glBindFramebuffer(GL_FRAMEBUFFER, g_fbo_t[1]); glViewport(0, 0, w, h);
        glUseProgram(g_prog_warp); bind_tex(0, g_tex[0]); bind_tex(1, g_tex_me);
        glUniform1i(u_w_cur, 0); glUniform1i(u_w_mv, 1); glUniform2f(u_w_rng, REACH / g_lw, REACH / g_lh); glUniform2f(u_w_th, g_th0, g_th1);
        glUniform1i(u_w_tint, 0); glUniform1i(u_w_nn, g_nn);
        draw_full();
        glUseProgram(g_prog_mix); bind_tex(0, g_tex[0]); bind_tex(1, g_tex[1]); bind_tex(2, g_tex_me);
        glUniform1i(u_mix_prev, 0); glUniform1i(u_mix_cur, 1); glUniform1i(u_mix_mv, 2); glUniform2f(u_mix_rng, REACH / g_lw, REACH / g_lh); glUniform1i(u_mix_tint, 0);
        draw_full();
        glUseProgram(g_prog_copy); bind_tex(0, g_tex[0]); glUniform1i(u_copy_tex, 0);
        draw_full();
        glFinish();
    }
    restore_state();
    while (glGetError() != GL_NO_ERROR) {}
    g_warm_w = w; g_warm_h = h; g_have_prev = 0; g_cur = 0;
    LOGI("warm-up %dx%d done in %.1f ms", w, h, (now_ns() - t0) / 1e6);
}

// ---- runtime user mode (in-game Quick Menu, JNI below) ----
// ALL_FG=1 only says "FG is available in this game" (MobileGlues, no SurfaceView). Whether it runs is the user mode:
// 0 Off = the hook does nothing at all (no GL work, no extra swap, nothing allocated), 1 Adaptive = the gate decides.
// Initial value: env ALL_FG_USER ("0" = off, anything else/absent = on, which keeps the old env-only behaviour).
static volatile int g_user = -1;     // -1 not decided yet
static int g_inited, g_last_user;
static volatile int g_status_active; // mirrors g_active for the status call (written by the render thread only)

void fg_set_user_mode(int on) { g_user = on ? 1 : 0; }

// 0 not available / failed, 1 off (user), 2 on, gate idle, 3 generating frames, 4 on, waiting for the first frame
int fg_status(void) {
    if (g_state == 0) return 0;
    if (g_state < 0) return 4;
    if (g_user == 0) return 1;
    if (!g_inited) return 4;
    return g_status_active ? 3 : 2;
}

void fg_before_swap(EGLDisplay dpy, EGLSurface surface) {
    if (g_state == 0) return;
    if (g_state < 0) {
        const char* e = getenv("ALL_FG");
        g_state = (e && e[0] == '1') ? 1 : 0;
        if (!g_state) return;
        if (g_user < 0) { const char* u = getenv("ALL_FG_USER"); g_user = (u && u[0] == '0') ? 0 : 1; }
    }
    int user = g_user;                    // read once: the mode may change from another thread at any time
    if (!user) {
        // Off: give back what FG holds (this thread has the GL context) and behave exactly like a game without FG
        if (g_last_user) { if (g_inited && g_tex[0]) release(); g_w = 0; g_warm_w = 0; g_active = 0; g_status_active = 0; g_have_prev = 0; g_last_user = 0; LOGI("Frame Generation switched off by the user"); }
        return;
    }
    if (!g_inited) {
        if (!init()) { LOGE("init failed, Frame Generation disabled"); g_state = 0; return; }
        g_inited = 1;
        LOGI("Frame Generation enabled (v3: adaptive, fixed-budget motion search)");
    }
    if (!g_last_user) {                   // just switched on: restart the adaptive gate from scratch (warm-up runs below)
        g_last_user = 1; g_active = 0; g_below = 0; g_probe_left = 0; g_probe_n = 0; g_act_n = 0; g_fail_streak = 0; g_backoff_ns = 0; g_since_ns = 0; g_last_ns = 0; g_have_prev = 0;
        LOGI("Frame Generation switched on by the user");
    }
    if (g_lat) lat_probe(dpy, surface);
    long long t0 = now_ns();
    if (g_lat >= 2 && g_swt_n < 512) g_swt[g_swt_n++] = t0;   // real-frame swap times (CLOCK_MONOTONIC, same clock as SurfaceFlinger) for swap->present analysis
    if (!g_fg) { g_active = 0; g_status_active = 0; g_have_prev = 0; g_last_ns = t0; stats_tick(0); return; }
    g_last_dt = 0;
    if (g_last_ns) { double dt = (t0 - g_last_ns) / 1e6; double a = g_probe_left > 0 ? 0.35 : 0.1; if (dt < 250.0) g_last_dt = dt; if (dt < 250.0) g_period_ms = g_period_ms * (1.0 - a) + dt * a; }
    g_last_ns = t0;
    g_real++;
    int was_active = g_active;
    update_gate(t0);
    g_status_active = g_active;
    if (g_active != was_active) g_switches++;
    if (g_warm_w == 0) warm(dpy, surface);   // allocate + run every pass once at world load, so the first active frame has no stall
    if (!g_active) { g_have_prev = 0; stats_tick(0); return; }   // game fast enough or too slow: zero GL work, no extra swap

    EGLint w = 0, h = 0;
    if (!eglQuerySurface_p(dpy, surface, EGL_WIDTH, &w) || !eglQuerySurface_p(dpy, surface, EGL_HEIGHT, &h) || w < 64 || h < 64) return;
    while (glGetError() != GL_NO_ERROR) {}
    g_frames_active++;

    long long ts = now_ns();
#define CPU(i) do { long long n_ = now_ns(); g_cpu[i] += (n_ - ts) / 1e6; ts = n_; } while (0)
    int newsize = (w != g_w || h != g_h);
    if (newsize || g_ss_age >= g_ssevery) { save_state(); g_ss_age = 0; } else g_ss_age++;
    CPU(0);

    int ok = 1;
    if (newsize) ok = resize(w, h);
    if (ok) {
        glDisable(GL_BLEND); glDisable(GL_DEPTH_TEST); glDisable(GL_STENCIL_TEST); glDisable(GL_SCISSOR_TEST);
        glDisable(GL_CULL_FACE); glDisable(GL_DITHER); glDisable(GL_RASTERIZER_DISCARD);
        glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
        glBindVertexArray(g_vao);
        long long tp = now_ns();
#define PROF(i) do { if (g_prof) { glFinish(); long long n_ = now_ns(); g_gpu_prof[i] += (n_ - tp) / 1e6; tp = n_; } } while (0)

        // 1) copy the finished real frame (default framebuffer) into texture[cur]
        if (g_blit) blit_full(0, g_fbo_t[g_cur], w, h);
        else { glBindFramebuffer(GL_READ_FRAMEBUFFER, 0); bind_tex(0, g_tex[g_cur]); glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h); }
        int prev = 1 - g_cur;
        CPU(1); PROF(0);
        // extrapolation mode: the real frame is presented NOW (no added latency); the generated frame is drawn into the
        // next back buffer below and is presented by the caller's normal swap half a period later
        int ext = (g_mode == 1 && g_have_prev && g_bench != 2 && g_bench != 3);   // bench 3: copy+luma+ME only (cost test, no generated frame)
        if (ext) eglSwapBuffers_p(dpy, surface);

        if (g_bench != 2) {
            // 1b) fixed-size luma copy of the new frame (flow resolution does not grow with the screen)
            glBindFramebuffer(GL_FRAMEBUFFER, g_fbo_lum[g_cur]);
            glViewport(0, 0, g_lw, g_lh);
            GLuint lp = g_lv ? g_prog_luma1 : g_prog_luma;
            glUseProgram(lp);
            bind_tex(0, g_tex[g_cur]);
            glUniform1i(glGetUniformLocation(lp, "uTex"), 0);
            glUniform2f(glGetUniformLocation(lp, "uOff"), 0.25f / g_lw, 0.25f / g_lh);
            draw_full();
        }
        CPU(2); PROF(1);

        if (g_have_prev && g_bench != 2) {
            // 2) motion search prev -> cur on a coarse grid of the luma copy
            glBindFramebuffer(GL_FRAMEBUFFER, g_fbo_me);
            glViewport(0, 0, g_mw, g_mh);
            {
                GLuint mp = g_mev ? g_prog_me : g_prog_me0;
                glUseProgram(mp);
                bind_tex(0, g_lum[prev]); bind_tex(1, g_lum[g_cur]);
                glUniform1i(glGetUniformLocation(mp, "uPrev"), 0); glUniform1i(glGetUniformLocation(mp, "uCur"), 1);
                glUniform2f(glGetUniformLocation(mp, "uInv"), 1.0f / g_lw, 1.0f / g_lh);
                glUniform1f(glGetUniformLocation(mp, "uThr"), 0.03f);
            }
            draw_full();
            CPU(3); PROF(2);

            if (g_bench == 3) {
                // cost test only: nothing else is drawn
            } else if (ext) {
                // 3e) half-step forward warp of the real frame into the (new) back buffer; pace so it is shown mid-period
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glViewport(0, 0, w, h);
                glUseProgram(g_prog_warp);
                bind_tex(0, g_tex[g_cur]); bind_tex(1, g_tex_me);
                glUniform1i(u_w_cur, 0); glUniform1i(u_w_mv, 1);
                glUniform2f(u_w_rng, REACH / g_lw, REACH / g_lh);
                glUniform2f(u_w_th, g_th0, g_th1);
                glUniform1i(u_w_tint, g_tint); glUniform1i(u_w_nn, g_nn);
                if (g_bench != 4) draw_full();   // bench 4 = cost test only: the generated frame is NOT drawn (shows garbage), isolates warp cost from second-present cost
                g_gen++;
                CPU(4); PROF(3);
                if (g_pace == 1) {
                    // sleep a whole number of display refreshes (half period rounded down, minus a little for sleep overshoot):
                    // a sleep that lands between two vsyncs wastes up to one refresh and lowers both real fps and shown frames
                    double vs = 1000.0 / (g_hz > 30.0f ? g_hz : 60.0f);
                    double half = g_period_ms * g_pf; if (half > 33.3) half = 33.3;
                    double ms = (double)((int)(half / vs)) * vs - 1.5; if (ms < vs - 1.5) ms = vs - 1.5;
                    struct timespec ts_ = { 0, (long)(ms * 1e6) }; nanosleep(&ts_, NULL);
                }
                CPU(5);
            } else {
            // 3) motion-compensated in-between frame to the window, present it
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, w, h);
            glUseProgram(g_prog_mix);
            bind_tex(0, g_tex[prev]); bind_tex(1, g_tex[g_cur]); bind_tex(2, g_tex_me);
            glUniform1i(u_mix_prev, 0); glUniform1i(u_mix_cur, 1); glUniform1i(u_mix_mv, 2);
            glUniform2f(u_mix_rng, REACH / g_lw, REACH / g_lh);
            glUniform1i(u_mix_tint, g_tint);
            draw_full();
            CPU(4);
            //swap interval is left as the game set it: forcing 1 persisted after FG paused and capped the game at the compositor's idle rate
            eglSwapBuffers_p(dpy, surface);
            g_gen++;
            CPU(5); PROF(3);
            // pacing: show the real frame about half a real-frame period after the generated one, without stalling the game thread
            if (g_pace == 2 && p_presTime) {
                double half = g_period_ms * 0.5; if (half > 20.0) half = 20.0;
                p_presTime(dpy, surface, now_ns() + (long long)(half * 1e6));
            } else if (g_pace == 1) {
                double ms = g_period_ms * 0.5; if (ms > 16.6) ms = 16.6;
                struct timespec ts_ = { 0, (long)(ms * 1e6) }; nanosleep(&ts_, NULL);
            }

            // 4) put the real frame back so the caller's normal swap presents it
            if (g_blit) blit_full(g_fbo_t[g_cur], 0, w, h);
            else { glBindFramebuffer(GL_FRAMEBUFFER, 0); glUseProgram(g_prog_copy); bind_tex(0, g_tex[g_cur]); glUniform1i(u_copy_tex, 0); draw_full(); }
            CPU(6); PROF(4);
            }
        }
        g_have_prev = 1; g_cur = 1 - g_cur;
    }
    restore_state();
    CPU(7);
    if (g_lat >= 2 && g_swg_n < 512 && g_mode == 1) g_swg[g_swg_n++] = now_ns();

    GLenum err = glGetError();
    if (err != GL_NO_ERROR || !ok) {
        LOGE("GL error 0x%x during Frame Generation, disabling it (game continues normally)", err);
        g_state = 0;
        return;
    }
    stats_tick(0);
}

// ---- JNI: in-game Quick Menu (com.movtery.zalithlauncher.all.fg.AllFgBridge) ----
JNIEXPORT void JNICALL Java_com_movtery_zalithlauncher_all_fg_AllFgBridge_nativeSetMode(JNIEnv* env, jclass clazz, jint on) {
    (void)env; (void)clazz;
    fg_set_user_mode(on != 0);
}
JNIEXPORT jint JNICALL Java_com_movtery_zalithlauncher_all_fg_AllFgBridge_nativeStatus(JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    return fg_status();
}
JNIEXPORT void JNICALL Java_com_movtery_zalithlauncher_all_fg_AllFgBridge_nativeSetHz(JNIEnv* env, jclass clazz, jfloat hz) {
    (void)env; (void)clazz;
    if (hz >= 30.0f && hz <= 360.0f) { g_hz_base = hz; g_hz = hz; }
}
