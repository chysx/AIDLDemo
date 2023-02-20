// INumCallback.aidl
package com.chintan.aidlserver;

// Declare any non-default types here with import statements

interface INumCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void call(int num);
}