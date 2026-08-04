#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <android/log.h>

#define LOG_TAG "PtyNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jint JNICALL
Java_com_devwithzachary_completelinuxinstaller_engine_PtyNative_createSubprocess(
    JNIEnv *env,
    jobject thiz,
    jstring cmdPath,
    jobjectArray jArgs,
    jobjectArray jEnv,
    jstring cwdPath,
    jint cols,
    jint rows,
    jintArray jOutPid) {

    int master_fd = posix_openpt(O_RDWR | O_NOCTTY);
    if (master_fd < 0) {
        LOGE("posix_openpt failed");
        return -1;
    }

    if (grantpt(master_fd) != 0 || unlockpt(master_fd) != 0) {
        LOGE("grantpt or unlockpt failed");
        close(master_fd);
        return -1;
    }

    char *pts_name = ptsname(master_fd);
    if (!pts_name) {
        LOGE("ptsname failed");
        close(master_fd);
        return -1;
    }

    // Set initial window size
    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_col = cols > 0 ? cols : 80;
    ws.ws_row = rows > 0 ? rows : 24;
    ioctl(master_fd, TIOCSWINSZ, &ws);

    // Prepare arguments
    int argc = env->GetArrayLength(jArgs);
    char **argv = (char **) malloc(sizeof(char *) * (argc + 1));
    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring) env->GetObjectArrayElement(jArgs, i);
        const char *cArg = env->GetStringUTFChars(arg, NULL);
        argv[i] = strdup(cArg);
        env->ReleaseStringUTFChars(arg, cArg);
        env->DeleteLocalRef(arg);
    }
    argv[argc] = NULL;

    // Prepare environment
    int envc = jEnv ? env->GetArrayLength(jEnv) : 0;
    char **envp = (char **) malloc(sizeof(char *) * (envc + 1));
    for (int i = 0; i < envc; i++) {
        jstring ev = (jstring) env->GetObjectArrayElement(jEnv, i);
        const char *cEv = env->GetStringUTFChars(ev, NULL);
        envp[i] = strdup(cEv);
        env->ReleaseStringUTFChars(ev, cEv);
        env->DeleteLocalRef(ev);
    }
    envp[envc] = NULL;

    const char *cCwd = cwdPath ? env->GetStringUTFChars(cwdPath, NULL) : NULL;
    const char *cCmd = env->GetStringUTFChars(cmdPath, NULL);

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork failed");
        close(master_fd);
        return -1;
    }

    if (pid == 0) {
        // Child Process
        close(master_fd);
        setsid();

        int slave_fd = open(pts_name, O_RDWR);
        if (slave_fd < 0) {
            exit(1);
        }

#ifdef TIOCSCTTY
        ioctl(slave_fd, TIOCSCTTY, 0);
#endif

        dup2(slave_fd, 0);
        dup2(slave_fd, 1);
        dup2(slave_fd, 2);

        if (slave_fd > 2) {
            close(slave_fd);
        }

        if (cCwd && strlen(cCwd) > 0) {
            chdir(cCwd);
        }

        execve(cCmd, argv, envp);
        exit(1);
    }

    // Parent Process
    env->ReleaseStringUTFChars(cmdPath, cCmd);
    if (cwdPath) env->ReleaseStringUTFChars(cwdPath, cCwd);

    for (int i = 0; i < argc; i++) free(argv[i]);
    free(argv);
    for (int i = 0; i < envc; i++) free(envp[i]);
    free(envp);

    jint pidArr[1] = { (jint) pid };
    env->SetIntArrayRegion(jOutPid, 0, 1, pidArr);

    return master_fd;
}

JNIEXPORT void JNICALL
Java_com_devwithzachary_completelinuxinstaller_engine_PtyNative_setPtyWindowSize(
    JNIEnv *env,
    jobject thiz,
    jint masterFd,
    jint cols,
    jint rows) {

    struct winsize ws;
    memset(&ws, 0, sizeof(ws));
    ws.ws_col = cols;
    ws.ws_row = rows;
    ioctl(masterFd, TIOCSWINSZ, &ws);
}

JNIEXPORT void JNICALL
Java_com_devwithzachary_completelinuxinstaller_engine_PtyNative_closeFd(
    JNIEnv *env,
    jobject thiz,
    jint masterFd) {
    if (masterFd >= 0) {
        close(masterFd);
    }
}

JNIEXPORT jint JNICALL
Java_com_devwithzachary_completelinuxinstaller_engine_PtyNative_waitForProcess(
    JNIEnv *env,
    jobject thiz,
    jint pid) {
    int status = 0;
    waitpid((pid_t) pid, &status, 0);
    return status;
}

} // extern "C"
