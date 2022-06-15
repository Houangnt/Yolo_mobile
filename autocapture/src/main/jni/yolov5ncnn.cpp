

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "yolov5.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static int draw_unsupported(cv::Mat& rgb)
{
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat& rgb)
{
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        for (int i = 0; i < 10; i++)
        {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0;
    int x = rgb.cols - label_size.width;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

static Yolov5* g_yolov5 = 0;
static ncnn::Mutex lock;

class MyNdkCamera : public NdkCameraWindow
{
public:
    virtual void on_image_render(cv::Mat& rgb) const ;
    virtual int* get_corners(int* corners) const;
    virtual bool has_face();
};

bool MyNdkCamera::has_face() {
    int len_obj = (g_yolov5->corners).size();
    for(int i=0;i<len_obj;i++){
        const Object &obj = (g_yolov5->corners)[i];
        int lb = obj.label;
        if(lb == 4){
            g_yolov5->has_face = true;
            return g_yolov5->has_face;
        }
    }
    return  g_yolov5->has_face = false;
}
int* MyNdkCamera::get_corners(int *corners) const {
    int len_obj = (g_yolov5->corners).size();
    int len_coors = 10;
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn_corners", "size %d", len_obj);

    for (int i = 0; i < len_coors - 2; ++i) {
        (g_yolov5->corner_values)[i] = -1;
    }


    for (int i = 0; i < len_obj; ++i) {
        const Object &obj = (g_yolov5->corners)[i];
        int lb = obj.label;
        if(lb == 4) {
            __android_log_print(ANDROID_LOG_DEBUG, "ncnn_corners", "label %d", lb);
            continue;
        } else {
            (g_yolov5->corner_values)[lb * 2] = obj.rect.x + obj.rect.width / 2;
            (g_yolov5->corner_values)[lb * 2 + 1] = obj.rect.y + obj.rect.height / 2;
        }
    }

    int w = g_yolov5->rgb_w;
    int h = g_yolov5->rgb_h;
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn_corners", "size rgb 2 {%d, %d}", w, h);
    (g_yolov5->corner_values)[8] = w;
    (g_yolov5->corner_values)[9] = h;

//    if( len_obj == 4) {
//        int x1 = 0, x2 = 0, x3 = 0, x4 = 0, y1 = 0, y2 = 0, y3 = 0, y4 = 0;
//        int i = 0;
//        for (; i < 4; i ++){
//            const Object& obj = (g_yolox->corners)[i];
//            switch (obj.label) {
//                case 0:
//                    x1 = obj.rect.x + obj.rect.width / 2;
//                    y1 = obj.rect.y + obj.rect.height / 2;
//                    break;
//                case 1:
//                    x2 = obj.rect.x + obj.rect.width / 2;
//                    y2 = obj.rect.y + obj.rect.height / 2;
//                    break;
//                case 2:
//                    x3 = obj.rect.x + obj.rect.width / 2;
//                    y3 = obj.rect.y + obj.rect.height / 2;
//                    break;
//                case 3:
//                    x4 = obj.rect.x + obj.rect.width / 2;
//                    y4 = obj.rect.y + obj.rect.height / 2;
//                    break;
//
//            }
//        }
//        int w = g_yolox->rgb_w;
//        int h = g_yolox->rgb_h;
//        int cors[10] = {x1, y1, x2, y2, x3, y3, x4, y4, w, h};
//        (g_yolox->corner_values)[0] = x1;
//        (g_yolox->corner_values)[1] = y1;
//        (g_yolox->corner_values)[2] = x2;
//        (g_yolox->corner_values)[3] = y2;
//        (g_yolox->corner_values)[4] = x3;
//        (g_yolox->corner_values)[5] = y3;
//        (g_yolox->corner_values)[6] = x4;
//        (g_yolox->corner_values)[7] = y4;
//        (g_yolox->corner_values)[8] = w;
//        (g_yolox->corner_values)[9] = h;
//
//
//        corners = cors;
//        __android_log_print(ANDROID_LOG_DEBUG, "ncnn_corners", "coor [%d, %d, %d, %d %d %d %d %d]", x1, y1, x2, y2, x3, y3, x4, y4);
//        __android_log_print(ANDROID_LOG_DEBUG, "ncnn_corners", "coor wh [%d, %d]", w, h);
//
//    } else {
//        int cors[10] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//        int i;
//        for(i=0; i<10;i++){
//            (g_yolox->corner_values)[i] = 0;
//        }
//        corners = cors;
//    }
    return g_yolov5->corner_values;
}
void MyNdkCamera::on_image_render(cv::Mat& rgb) const
{
    {
        ncnn::MutexLockGuard g(lock);

        if (g_yolov5)
        {
            std::vector<Object> objects;
            g_yolov5->detect(rgb, objects);
            g_yolov5->corners = objects;
            g_yolov5->rgb_w = rgb.cols;
            g_yolov5->rgb_h = rgb.rows;
            g_yolov5->draw(rgb, objects);
        }
        else
        {
            draw_unsupported(rgb);
        }
    }

   // draw_fps(rgb);
}

static MyNdkCamera* g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yolov5;
        g_yolov5 = 0;
    }

    delete g_camera;
    g_camera = 0;
}

JNIEXPORT jboolean JNICALL Java_com_example_autocapture_NcnnYolov5_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu)
{
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
    {
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char* modeltypes[] =
    {
        "yolov5",
    };

    const int target_sizes[] =
    {
        416
    };

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];
    bool use_gpu = (int)cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0)
        {
            // no gpu
            delete g_yolov5;
            g_yolov5 = 0;
        }
        else
        {
            if (!g_yolov5)
                g_yolov5 = new Yolov5;
            g_yolov5->load(mgr, modeltype, target_size, use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL Java_com_example_autocapture_NcnnYolov5_openCamera(JNIEnv* env, jobject thiz, jint facing)
{
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int)facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL Java_com_example_autocapture_NcnnYolov5_closeCamera(JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL Java_com_example_autocapture_NcnnYolov5_setOutputWindow(JNIEnv* env, jobject thiz, jobject surface)
{
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}
JNIEXPORT jboolean JNICALL Java_com_example_autocapture_NcnnYolov5_hasFace(JNIEnv* env, jobject thiz)
{
    bool hasface = (g_camera->has_face());
    if(hasface == true) {
        return JNI_TRUE;
    } else
        return JNI_FALSE;
}
// public native boolean getCorners();
JNIEXPORT jintArray JNICALL Java_com_example_autocapture_NcnnYolov5_getCorners(JNIEnv* env, jobject thiz, jintArray corners)
{
    jint coor_jni [12];
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "getCorners");

    int* coor = (g_camera->get_corners(reinterpret_cast<int *>(corners)));

    for (int i = 0; i < 12; i ++){
        coor_jni[i] = coor[i];
    }
   // if(coor_jni[8] < coor_jni[9]){
    if(coor_jni[10] < coor_jni[11]){
        __android_log_print(ANDROID_LOG_DEBUG, "ncnn_corners", "reverse order");
        int temp;
        for(int i = 0; i < 8; ++ i){
           if(i % 2 == 0){
               temp = coor_jni[i];
               coor_jni[i] = - coor_jni[i + 1];
               coor_jni[i + 1] = temp;
            }
        }
        temp = coor_jni[8];
        coor_jni[8] = coor_jni[9];
        coor_jni[9] = temp;

        for(int i = 0; i < 8; ++ i){
            if(i % 2 == 0){
                coor_jni[i] = coor_jni[i] + coor_jni[8];
            }
        }
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, "ncnn_corners", "preserve order");
    }
    jintArray  result = env->NewIntArray(12);
    env->SetIntArrayRegion(result, 0, 12, coor_jni);
    return result;
}
}
