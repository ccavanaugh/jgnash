/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

/**
 * A class containing static math methods useful for image processing.
 */
class ImageMath {


    /**
     * Premultiply a block of pixels
     */
    public static void premultiply(int[] p, int offset, int length) {
        length += offset;
        for (int i = offset; i < length; i++) {
            int rgb = p[i];
            int a = rgb >> 24 & 0xff;
            int r = rgb >> 16 & 0xff;
            int g = rgb >> 8 & 0xff;
            int b = rgb & 0xff;
            float f = a * 1.0f / 255.0f;
            r *= f;
            g *= f;
            b *= f;
            p[i] = a << 24 | r << 16 | g << 8 | b;
        }
    }

    /**
     * Premultiply a block of pixels
     */
    public static void unpremultiply(int[] p, int offset, int length) {
        length += offset;
        for (int i = offset; i < length; i++) {
            int rgb = p[i];
            int a = rgb >> 24 & 0xff;
            int r = rgb >> 16 & 0xff;
            int g = rgb >> 8 & 0xff;
            int b = rgb & 0xff;
            if (a != 0 && a != 255) {
                float f = 255.0f / a;
                r *= f;
                g *= f;
                b *= f;
                if (r > 255) {
                    r = 255;
                }
                if (g > 255) {
                    g = 255;
                }
                if (b > 255) {
                    b = 255;
                }
                p[i] = a << 24 | r << 16 | g << 8 | b;
            }
        }
    }
}
