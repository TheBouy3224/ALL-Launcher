// Generated: MobileGlues desktop-GL entry points loaded by dlsym from libmobileglues.so.
// Frame Generation runs as a desktop-GL client of MobileGlues, which translates it to GLES; it cannot use the system GLES directly.
#ifndef ALL_FG_GL_H
#define ALL_FG_GL_H
#include <GLES3/gl3.h>
#define FG_GL_FUNCS(X) \
  X(glActiveTexture) \
  X(glAttachShader) \
  X(glBindFramebuffer) \
  X(glBindSampler) \
  X(glBindTexture) \
  X(glBindVertexArray) \
  X(glCheckFramebufferStatus) \
  X(glColorMask) \
  X(glCompileShader) \
  X(glCopyTexSubImage2D) \
  X(glCreateProgram) \
  X(glCreateShader) \
  X(glDeleteProgram) \
  X(glDeleteShader) \
  X(glDeleteTextures) \
  X(glDisable) \
  X(glDrawArrays) \
  X(glEnable) \
  X(glFinish) \
  X(glFramebufferTexture2D) \
  X(glGenFramebuffers) \
  X(glGenTextures) \
  X(glGenVertexArrays) \
  X(glGetBooleanv) \
  X(glGetError) \
  X(glGetIntegerv) \
  X(glGetProgramInfoLog) \
  X(glGetProgramiv) \
  X(glGetShaderInfoLog) \
  X(glGetShaderiv) \
  X(glGetUniformLocation) \
  X(glIsEnabled) \
  X(glLinkProgram) \
  X(glShaderSource) \
  X(glTexImage2D) \
  X(glTexParameteri) \
  X(glUniform1f) \
  X(glUniform1i) \
  X(glUniform2f) \
  X(glUseProgram) \
  X(glViewport) \
  X(glBlitFramebuffer) \
  X(glReadPixels) \
  X(glGenBuffers) \
  X(glBindBuffer) \
  X(glBufferData) \
  X(glMapBufferRange) \
  X(glUnmapBuffer) \

extern __typeof__(&glActiveTexture) fp_glActiveTexture;
extern __typeof__(&glAttachShader) fp_glAttachShader;
extern __typeof__(&glBindFramebuffer) fp_glBindFramebuffer;
extern __typeof__(&glBindSampler) fp_glBindSampler;
extern __typeof__(&glBindTexture) fp_glBindTexture;
extern __typeof__(&glBindVertexArray) fp_glBindVertexArray;
extern __typeof__(&glCheckFramebufferStatus) fp_glCheckFramebufferStatus;
extern __typeof__(&glColorMask) fp_glColorMask;
extern __typeof__(&glCompileShader) fp_glCompileShader;
extern __typeof__(&glCopyTexSubImage2D) fp_glCopyTexSubImage2D;
extern __typeof__(&glCreateProgram) fp_glCreateProgram;
extern __typeof__(&glCreateShader) fp_glCreateShader;
extern __typeof__(&glDeleteProgram) fp_glDeleteProgram;
extern __typeof__(&glDeleteShader) fp_glDeleteShader;
extern __typeof__(&glDeleteTextures) fp_glDeleteTextures;
extern __typeof__(&glDisable) fp_glDisable;
extern __typeof__(&glDrawArrays) fp_glDrawArrays;
extern __typeof__(&glEnable) fp_glEnable;
extern __typeof__(&glFinish) fp_glFinish;
extern __typeof__(&glFramebufferTexture2D) fp_glFramebufferTexture2D;
extern __typeof__(&glGenFramebuffers) fp_glGenFramebuffers;
extern __typeof__(&glGenTextures) fp_glGenTextures;
extern __typeof__(&glGenVertexArrays) fp_glGenVertexArrays;
extern __typeof__(&glGetBooleanv) fp_glGetBooleanv;
extern __typeof__(&glGetError) fp_glGetError;
extern __typeof__(&glGetIntegerv) fp_glGetIntegerv;
extern __typeof__(&glGetProgramInfoLog) fp_glGetProgramInfoLog;
extern __typeof__(&glGetProgramiv) fp_glGetProgramiv;
extern __typeof__(&glGetShaderInfoLog) fp_glGetShaderInfoLog;
extern __typeof__(&glGetShaderiv) fp_glGetShaderiv;
extern __typeof__(&glGetUniformLocation) fp_glGetUniformLocation;
extern __typeof__(&glIsEnabled) fp_glIsEnabled;
extern __typeof__(&glLinkProgram) fp_glLinkProgram;
extern __typeof__(&glShaderSource) fp_glShaderSource;
extern __typeof__(&glTexImage2D) fp_glTexImage2D;
extern __typeof__(&glTexParameteri) fp_glTexParameteri;
extern __typeof__(&glUniform1f) fp_glUniform1f;
extern __typeof__(&glUniform1i) fp_glUniform1i;
extern __typeof__(&glUniform2f) fp_glUniform2f;
extern __typeof__(&glUseProgram) fp_glUseProgram;
extern __typeof__(&glViewport) fp_glViewport;
extern __typeof__(&glBlitFramebuffer) fp_glBlitFramebuffer;
extern __typeof__(&glReadPixels) fp_glReadPixels;
extern __typeof__(&glGenBuffers) fp_glGenBuffers;
extern __typeof__(&glBindBuffer) fp_glBindBuffer;
extern __typeof__(&glBufferData) fp_glBufferData;
extern __typeof__(&glMapBufferRange) fp_glMapBufferRange;
extern __typeof__(&glUnmapBuffer) fp_glUnmapBuffer;
int fg_gl_load(void);
#ifndef FG_GL_TABLE_IMPL
#define glActiveTexture fp_glActiveTexture
#define glAttachShader fp_glAttachShader
#define glBindFramebuffer fp_glBindFramebuffer
#define glBindSampler fp_glBindSampler
#define glBindTexture fp_glBindTexture
#define glBindVertexArray fp_glBindVertexArray
#define glCheckFramebufferStatus fp_glCheckFramebufferStatus
#define glColorMask fp_glColorMask
#define glCompileShader fp_glCompileShader
#define glCopyTexSubImage2D fp_glCopyTexSubImage2D
#define glCreateProgram fp_glCreateProgram
#define glCreateShader fp_glCreateShader
#define glDeleteProgram fp_glDeleteProgram
#define glDeleteShader fp_glDeleteShader
#define glDeleteTextures fp_glDeleteTextures
#define glDisable fp_glDisable
#define glDrawArrays fp_glDrawArrays
#define glEnable fp_glEnable
#define glFinish fp_glFinish
#define glFramebufferTexture2D fp_glFramebufferTexture2D
#define glGenFramebuffers fp_glGenFramebuffers
#define glGenTextures fp_glGenTextures
#define glGenVertexArrays fp_glGenVertexArrays
#define glGetBooleanv fp_glGetBooleanv
#define glGetError fp_glGetError
#define glGetIntegerv fp_glGetIntegerv
#define glGetProgramInfoLog fp_glGetProgramInfoLog
#define glGetProgramiv fp_glGetProgramiv
#define glGetShaderInfoLog fp_glGetShaderInfoLog
#define glGetShaderiv fp_glGetShaderiv
#define glGetUniformLocation fp_glGetUniformLocation
#define glIsEnabled fp_glIsEnabled
#define glLinkProgram fp_glLinkProgram
#define glShaderSource fp_glShaderSource
#define glTexImage2D fp_glTexImage2D
#define glTexParameteri fp_glTexParameteri
#define glUniform1f fp_glUniform1f
#define glUniform1i fp_glUniform1i
#define glUniform2f fp_glUniform2f
#define glUseProgram fp_glUseProgram
#define glViewport fp_glViewport
#define glBlitFramebuffer fp_glBlitFramebuffer
#define glReadPixels fp_glReadPixels
#define glGenBuffers fp_glGenBuffers
#define glBindBuffer fp_glBindBuffer
#define glBufferData fp_glBufferData
#define glMapBufferRange fp_glMapBufferRange
#define glUnmapBuffer fp_glUnmapBuffer
#endif
#endif
