


#ifndef YOLOv5_H
#define YOLOv5_H

#include <opencv2/core/core.hpp>

#include <net.h>

struct Object
{
    cv::Rect_<float> rect;
    int label;
    float prob;
   
};


class Yolov5
{
public:
    Yolov5();

    int load(const char* modeltype, int target_size, const float* mean_vals, const float* norm_vals, bool use_gpu = false);

    int load(AAssetManager* mgr, const char* modeltype, int target_size, bool use_gpu = false);

    int detect(const cv::Mat& rgb, std::vector<Object>& objects, float prob_threshold = 0.45f, float nms_threshold = 0.65f);

    int draw(cv::Mat& rgb, const std::vector<Object>& objects);
public:
    std::vector<Object> corners;
    int rgb_w = 0;
    int rgb_h = 0;
    bool has_face;
    int corner_values[12] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,0};
private:

    ncnn::Net yolov5;

    int target_size;
    float mean_vals[3];
    float norm_vals[3];
    int image_w;
    int image_h;
    int in_w;
    int in_h;

    ncnn::UnlockedPoolAllocator blob_pool_allocator;
    ncnn::PoolAllocator workspace_pool_allocator;
};

#endif
