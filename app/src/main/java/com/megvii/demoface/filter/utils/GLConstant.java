package com.megvii.demoface.filter.utils;

/**
 * @author zwq
 */
public @interface GLConstant {

    float[] VERTEX_CUBE = {
            -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f
    };

    float[] VERTEX = {
            -1.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f,
            -1.0f, -1.0f,
    };


    float[] VERTEX2 = {
            -0.5f, 0.5f,
            0.5f, 0.5f,
            0.5f, -0.5f,
            -0.5f, -0.5f,
    };

    @Deprecated
    float[] VERTEX_SQUARE = VERTEX;

    float[] TEXTURE = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
    };

    float[] TEXTURE_V2 = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f
    };

    short[] VERTEX_INDEX = {
            0, 1, 2,
            0, 2, 3
    };

    short[] VERTEX_INDEX_V2 = {
            2, 3, 1,
            2, 1, 0
    };

    @Deprecated
    short[] VERTEX_INDEX_DFILTER = VERTEX_INDEX_V2;
}
