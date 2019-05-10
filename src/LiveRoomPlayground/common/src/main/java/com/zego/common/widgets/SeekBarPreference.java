package com.zego.common.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zego.common.R;


public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    private TextView value, title;
    private int mProgress = 100;
    private int mMax = 100;//如果您的seekbar最大值超过了10000，那么在这里修改下即可。否则会被限制最大值为10000
    private boolean mTrackingTouch;
    private OnSeekBarPrefsChangeListener mListener = null;

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);


        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.customSeekBar);
        typedArray.getInteger(R.styleable.customSeekBar_current, 100);
        Integer max=typedArray.getInteger(R.styleable.customSeekBar_max, 100);
        typedArray.getInteger(R.styleable.customSeekBar_min, 0);
        setMax(mMax);
        setLayoutResource(R.layout.seekbar_prefs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        seekBar.setMax(mMax);
        seekBar.setProgress(mProgress);
        seekBar.setEnabled(isEnabled());
        seekBar.setOnSeekBarChangeListener(this);
        title = view.findViewById(R.id.seek_bar_title);
        value = view.findViewById(R.id.value);
        value.setText(String.valueOf(mProgress));
        title.setText(getTitle());

    }

    /**
     * 设置默认的值
     *
     * @param defaultValue
     */
    public void setDefaultProgressValue(int defaultValue) {
        if (getPersistedInt(-1) == -1) {
            //说明没有用户设定的值，所以设置初始值
            setProgress(defaultValue);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setProgress(restoreValue ? getPersistedInt(mProgress) : (Integer) defaultValue);
    }

    public void setMax(int max) {
        if (max != mMax) {
            mMax = max;
            notifyChanged();
        }
    }

    public void setProgress(int progress) {
        setProgress(progress, true);
    }

    private void setProgress(int progress, boolean notifyChanged) {
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress < 0) {
            progress = 0;
        }
        if (progress != mProgress) {
            mProgress = progress;
            persistInt(progress);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    public int getProgress() {
        return mProgress;
    }

    public void setOnSeekBarPrefsChangeListener(OnSeekBarPrefsChangeListener listener) {
        mListener = listener;
    }

    /**
     * @author:Jack Tony
     * @tips :设置监听器
     * @date :2014-8-17
     */
    public interface OnSeekBarPrefsChangeListener {
        //public void OnSeekBarChangeListener(SeekBar seekBar, boolean isChecked);
        public void onStopTrackingTouch(String key, SeekBar seekBar);

        public void onStartTrackingTouch(String key, SeekBar seekBar);

        public void onProgressChanged(String key, SeekBar seekBar, int progress, boolean fromUser);
    }

    /**
     * Persist the seekBar's progress value if callChangeListener
     * returns true, otherwise set the seekBar's progress to the stored value
     */
    void syncProgress(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (progress != mProgress) {
            if (callChangeListener(progress)) {
                setProgress(progress, false);
            } else {
                seekBar.setProgress(mProgress);
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //value.setText(seekBar.getProgress()+"");
        if (mListener != null) {
            mListener.onProgressChanged(getKey(), seekBar, progress, fromUser);
        }
        if (seekBar.getProgress() != mProgress) {
            syncProgress(seekBar);
        }
        if (fromUser && !mTrackingTouch) {
            syncProgress(seekBar);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mListener != null) {
            mListener.onStartTrackingTouch(getKey(), seekBar);
        }
        mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mListener != null) {
            mListener.onStopTrackingTouch(getKey(), seekBar);
        }
        mTrackingTouch = false;
        if (seekBar.getProgress() != mProgress) {
            syncProgress(seekBar);
        }
        notifyHierarchyChanged();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /*
         * Suppose a client uses this preference type without persisting. We
         * must save the instance state so it is able to, for example, survive
         * orientation changes.
         */

        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.progress = mProgress;
        myState.max = mMax;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mProgress = myState.progress;

        mMax = myState.max;
        notifyChanged();
    }

    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state
     * of MyPreference, a subclass of Preference.
     * <p>
     * It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        int progress;
        int max;

        public SavedState(Parcel source) {
            super(source);
            // Restore the click counter
            progress = source.readInt();
            max = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Save the click counter
            dest.writeInt(progress);
            dest.writeInt(max);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

    }
}
