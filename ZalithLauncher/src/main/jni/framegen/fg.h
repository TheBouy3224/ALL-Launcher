// ALL Frame Generation: independent of ALL Render / shaders. Hooked from gl_swap_buffers() only.
#ifndef ALL_FG_H
#define ALL_FG_H
#include <EGL/egl.h>

// Called right before the real eglSwapBuffers of a frame. If Frame Generation is enabled (env ALL_FG=1) and
// working, it presents one generated frame in between and leaves the real frame ready in the default
// framebuffer, so the caller's own swap presents it. When disabled or after any failure it does nothing.
void fg_before_swap(EGLDisplay dpy, EGLSurface surface);

// Called from the input bridge when a key is pressed (latency probe, test key lat=1 only reads it).
void fg_note_input(void);

#endif
