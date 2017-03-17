package com.aricneto.twistytimer.layout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.TextView;

import com.aricneto.twistify.R;

/**
 * <p>
 * A text view that strikes out the value with an "X" drawn over the text when
 * the view is checked.
 * </p>
 * <p>
 * By default, the view is not checked; override the default state by calling
 * {@link #setChecked(boolean)} or {@link #toggle()} from code, or by setting
 * the {@code checked} attribute in the layout XML to {@code true} or
 * {@code false}. The default strike-out color is red; override the default
 * color by calling {@link #setStrikeoutColor(int)}, or by setting the
 * {@code strikeoutColor} attribute in the layout XML. The attributes inherited
 * from {@code android.widget.TextView} can be used to set the text size and
 * text color.
 * </p>
 * <p>
 * When checked, the strike-out "X" is drawn using strokes along the diagonals
 * of the text view's padded region (less a little extra vertical space above
 * and below), not the bounding box of the drawn text. It may be preferable to
 * set the {@code android:layout_width} attribute to {@code wrap_content} to
 * ensure the "X" aligns with the text. The stroke width of the "X" is set
 * automatically based on the text size set on the view and is increased if a
 * bold typeface is used.
 * </p>
 *
 * @author damo
 */
public class StrikeoutTextView extends TextView implements Checkable {
    /**
     * Indicates if the view is checked and the strike-out "X" should be drawn.
     */
    private boolean mIsChecked;

    /**
     * The paint resource to use for the strike-out.
     */
    private final Paint mStrikeoutPaint = new Paint();

    public StrikeoutTextView(Context context) {
        this(context, null, 0);
    }

    public StrikeoutTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StrikeoutTextView(Context context, AttributeSet attrs,
                             int  defStyle) {
        super(context, attrs, defStyle);

        final TypedArray ta = context.obtainStyledAttributes(
            attrs, R.styleable.StrikeoutTextView, defStyle, 0);

        setChecked(ta.getBoolean(R.styleable.StrikeoutTextView_checked, false));
        setStrikeoutColor(ta.getColor(
            R.styleable.StrikeoutTextView_strikeoutColor, Color.RED));

        ta.recycle();

        mStrikeoutPaint.setAntiAlias(true);
        mStrikeoutPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * Sets the color to use for the strike-out "X" drawn over the text.
     *
     * @param color The new strike-out color.
     */
    public void setStrikeoutColor(int color) {
        mStrikeoutPaint.setColor(color);
        invalidate();
    }

    /**
     * Gets the color used for the strike-out "X" drawn over the text. The
     * value can be changed by calling {@link #setStrikeoutColor(int)}.
     *
     * @return The current strike-out color.
     */
    public int getStrikeoutColor() {
        return mStrikeoutPaint.getColor();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isChecked()) {
            return;
        }

        // The "Typeface" may have a bold variant, but if the "bold" attribute
        // is set and the typeface has no such variant, then "TextView" will
        // turn on "fake bold". Therefore, check both possibilities. The stroke
        // width will not be less than one pixel.
        final Paint textPaint = getPaint();
        final float strokeScale
            = textPaint.isFakeBoldText() || textPaint.getTypeface().isBold()
                ? 0.13f : 0.09f;
        final float strokeWidth
            = Math.max(1f, textPaint.getTextSize() * strokeScale);

        mStrikeoutPaint.setStrokeWidth(strokeWidth);

        // NOTE: Perhaps "getClipBounds" could be used here instead.
        final float ll = getPaddingLeft();
        final float tt = getPaddingTop();
        final float rr = getWidth() - getPaddingRight();
        final float bb = getHeight() - getPaddingBottom();

        // It looks better if the "X" is not drawn to the full height of the
        // padded region. Insetting more at the top than the bottom also looks
        // better (at least for the use case in this app). Use half the stroke
        // width as the minimum inset on all sides to avoid clipping the end
        // cap of the stroke.
        final float strokeInset = strokeWidth / 2f;
        final float inset = Math.max(strokeInset, (bb - tt) / 10f);
        final float l = ll + strokeInset;
        final float t = tt + inset * 1.5f;
        final float r = rr - strokeInset;
        final float b = bb - inset;

        canvas.drawLine(l, t, r, b, mStrikeoutPaint); // "\" diagonal
        canvas.drawLine(r, t, l, b, mStrikeoutPaint); // "/" diagonal
    }

    /**
     * Sets the checked state of this text view.
     *
     * @param checked
     *     {@code true} to check the text view and show the strike-out "X"; or
     *     {@code false} to display as a normal text view.
     */
    @Override
    public void setChecked(boolean checked) {
        if (checked != mIsChecked) {
            toggle(); // Which calls "invalidate()".
        }
    }

    /**
     * Gets the checked state of this text view.
     *
     * @return
     *     {@code true} if the view is checked and is showing the strike-out
     *     "X"; or {@code false} if the view is not checked and struck out.
     */
    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Toggles the checked state of the text view to its opposite state. If the
     * state is not known, check {@link #isChecked()} or set the required state
     * by calling {@link #setChecked(boolean)}.
     */
    @Override
    public void toggle() {
        mIsChecked = !mIsChecked;
        invalidate();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(),
            isChecked(), getStrikeoutColor());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(((SavedState) state).getSuperState());
        setChecked(((SavedState) state).isChecked);
        setStrikeoutColor(((SavedState) state).strikeoutColor);
    }

    /**
     * The holder for the extra saved instance state (checked state and
     * strike-out color) of this view.
     */
    static class SavedState extends BaseSavedState {
        /** The checked state to be saved/restored. */
        boolean isChecked;

        /** The strike-out color to be saved/restored. */
        int strikeoutColor;

        SavedState(Parcelable superState,
                   boolean isChecked, int strikeoutColor) {
            super(superState);
            this.isChecked = isChecked;
            this.strikeoutColor = strikeoutColor;
        }

        private SavedState(Parcel source) {
            super(source);
            isChecked = source.readByte() == 1;
            strikeoutColor = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (isChecked ? 1 : 0));
            out.writeInt(strikeoutColor);
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
