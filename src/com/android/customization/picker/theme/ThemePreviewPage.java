package com.android.customization.picker.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.icu.text.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import com.android.customization.picker.BasePreviewAdapter.PreviewPage;
import com.android.wallpaper.R;

import java.text.FieldPosition;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

abstract class ThemePreviewPage extends PreviewPage {
    @StringRes
    final int nameResId;
    @DrawableRes
    final int iconSrc;
    @LayoutRes
    final int contentLayoutRes;
    @ColorInt
    final int accentColor;
    protected final LayoutInflater inflater;

    public ThemePreviewPage(Context context, @StringRes int titleResId,
            @DrawableRes int iconSrc, @LayoutRes int contentLayoutRes,
            @ColorInt int accentColor) {
        super(null);
        this.nameResId = titleResId;
        this.iconSrc = iconSrc;
        this.contentLayoutRes = contentLayoutRes;
        this.accentColor = accentColor;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public void bindPreviewContent() {
        TextView header = card.findViewById(R.id.theme_preview_card_header);
        header.setText(nameResId);
        header.setCompoundDrawablesWithIntrinsicBounds(0, iconSrc, 0, 0);
        header.setCompoundDrawableTintList(ColorStateList.valueOf(accentColor));
        card.findViewById(R.id.theme_preview_top_bar).setVisibility(View.GONE);
        card.findViewById(R.id.edit_label).setVisibility(View.GONE);

        ViewGroup body = card.findViewById(R.id.theme_preview_card_body_container);
        inflater.inflate(contentLayoutRes, body, true);
        bindBody(false);
    }

    protected boolean containsWallpaper() {
        return false;
    }

    protected abstract void bindBody(boolean forceRebind);

    static class ThemeCoverPage extends ThemePreviewPage {

        private final Typeface mHeadlineFont;
        private final List<Drawable> mIcons;
        private String mTitle;
        private OnClickListener mEditClickListener;
        private final OnLayoutChangeListener mListener;
        private final int mCornerRadius;

        public ThemeCoverPage(Context context, String title, int accentColor, List<Drawable> icons,
                Typeface headlineFont, int cornerRadius,
                OnClickListener editClickListener,
                OnLayoutChangeListener wallpaperListener) {
            super(context, 0, 0, R.layout.preview_card_cover_content, accentColor);
            mTitle = title;
            mHeadlineFont = headlineFont;
            mIcons = icons;
            mCornerRadius = cornerRadius;
            mEditClickListener = editClickListener;
            mListener = wallpaperListener;
        }

        @Override
        protected void bindBody(boolean forceRebind) {
            card.addOnLayoutChangeListener(mListener);
            if (forceRebind) {
                card.requestLayout();
            }
        }

        @Override
        public void bindPreviewContent() {
            TextView header = card.findViewById(R.id.theme_preview_card_header);
            header.setText(mTitle);
            header.setTextAppearance(R.style.CoverTitleTextAppearance);
            header.setTypeface(mHeadlineFont);

            card.findViewById(R.id.theme_preview_top_bar).setVisibility(View.VISIBLE);
            TextView clock = card.findViewById(R.id.theme_preview_clock);
            clock.setText(getFormattedTime());
            clock.setTypeface(mHeadlineFont);

            ViewGroup iconsContainer = card.findViewById(R.id.theme_preview_top_bar_icons);

            for (int i = 0; i < iconsContainer.getChildCount() && i < mIcons.size(); i++) {
                ((ImageView) iconsContainer.getChildAt(i))
                        .setImageDrawable(mIcons.get(i).getConstantState().newDrawable().mutate());
            }

            ViewGroup body = card.findViewById(R.id.theme_preview_card_body_container);

            inflater.inflate(contentLayoutRes, body, true);

            bindBody(false);

            TextView editLabel = card.findViewById(R.id.edit_label);
            editLabel.setOnClickListener(mEditClickListener);
            card.setOnClickListener(mEditClickListener);
            editLabel.setVisibility(mEditClickListener != null
                    ? View.VISIBLE : View.INVISIBLE);
            ColorStateList themeAccentColor = ColorStateList.valueOf(accentColor);
            editLabel.setTextColor(themeAccentColor);
            editLabel.setCompoundDrawableTintList(themeAccentColor);
            View qsb = card.findViewById(R.id.theme_qsb);
            if (qsb != null && qsb.getVisibility() == View.VISIBLE) {
                if (qsb.getBackground() instanceof GradientDrawable) {
                    GradientDrawable bg = (GradientDrawable) qsb.getBackground();
                    float cornerRadius = useRoundedQSB(mCornerRadius)
                            ? (float)qsb.getLayoutParams().height / 2 : mCornerRadius;
                    bg.setCornerRadii(new float[]{
                            cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                            cornerRadius, cornerRadius, cornerRadius, cornerRadius});
                }
            }
        }

        private boolean useRoundedQSB(int cornerRadius) {
            return cornerRadius >=
                    card.getResources().getDimensionPixelSize(R.dimen.roundCornerThreshold);
        }

        private String getFormattedTime() {
            DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
            StringBuffer time = new StringBuffer();
            FieldPosition amPmPosition = new FieldPosition(DateFormat.Field.AM_PM);
            df.format(Calendar.getInstance(TimeZone.getDefault()).getTime(), time, amPmPosition);
            if (amPmPosition.getBeginIndex() > 0) {
                time.delete(amPmPosition.getBeginIndex(), amPmPosition.getEndIndex());
            }
            return time.toString();
        }

        @Override
        protected boolean containsWallpaper() {
            return true;
        }
    }
}
