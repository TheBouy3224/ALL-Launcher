#include <dlfcn.h>
#define FG_GL_TABLE_IMPL
#include "fg_gl.h"
#define DEF(n) __typeof__(&n) fp_##n;
FG_GL_FUNCS(DEF)
int fg_gl_load(void) {
    void* h = dlopen("libmobileglues.so", RTLD_NOW | RTLD_NOLOAD);
    if (!h) return 0;
    int ok = 1;
#define LOAD(n) fp_##n = (__typeof__(&n))dlsym(h, #n); if (!fp_##n) ok = 0;
    FG_GL_FUNCS(LOAD)
    return ok;
}
