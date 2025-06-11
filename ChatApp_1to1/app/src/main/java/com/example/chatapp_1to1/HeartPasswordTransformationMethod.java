package com.example.chatapp_1to1;

import android.text.method.PasswordTransformationMethod;
import android.view.View;

public class HeartPasswordTransformationMethod extends PasswordTransformationMethod {

    @Override
    public CharSequence getTransformation(CharSequence source, View view) {
        return new HeartCharSequence(source);
    }

    private static class HeartCharSequence implements CharSequence {
        private final CharSequence mSource;

        public HeartCharSequence(CharSequence source) {
            mSource = source;
        }

        public char charAt(int index) {
            return '♡'; // 원하는 마스킹 문자
        }

        public int length() {
            return mSource.length();
        }

        public CharSequence subSequence(int start, int end) {
            return mSource.subSequence(start, end);
        }
    }
}
