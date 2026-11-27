package com.xlythe.saolauncher.smsextension.util;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class RetainFragment extends Fragment {
    
    private static final String TAG = "com.xlythe.saolauncher.smsextension.retainfragment";
    private Object mObject;
    
    public RetainFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
    public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
        final RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
        if (fragment != null) {
            return fragment;
        }
        return new RetainFragment();
    }
    
    public void setObject(Object obj) {
        mObject = obj;
    }
    
    public Object getObject() {
        return mObject;
    }

}
