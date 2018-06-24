package org.thoughtcrime.securesms.components.emoji;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.emoji.EmojiProvider.EmojiDrawable;
import org.thoughtcrime.securesms.components.emoji.parsing.EmojiParser;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;


public class EmojiTextView extends AppCompatTextView {

  private final boolean scaleEmojis;
  private final boolean singleLineFix;
  private final int     maxLines;

  private CharSequence  previousText;
  private BufferType    previousBufferType;
  private float         originalFontSize;
  private long          emojiUpdateTime;
  private int           emojiStyle;
  private boolean       useSystemEmoji;
  private boolean       sizeChangeInProgress;

  public EmojiTextView(Context context) {
    this(context, null);
  }

  public EmojiTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public EmojiTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    TypedArray a  = context.getTheme().obtainStyledAttributes(attrs, R.styleable.EmojiTextView, 0, 0);
    scaleEmojis   = a.getBoolean(R.styleable.EmojiTextView_scaleEmojis, false);
    singleLineFix = a.getBoolean(R.styleable.EmojiTextView_singleLineFix, false);
    a.recycle();

    a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.textSize});
    originalFontSize = a.getDimensionPixelSize(0, 0);
    a.recycle();

    a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.maxLines});
    maxLines = a.getInt(0, Integer.MAX_VALUE);
    a.recycle();
  }

  @Override public void setText(@Nullable CharSequence text, BufferType type) {
    EmojiProvider             provider   = EmojiProvider.getInstance(getContext());
    EmojiParser.CandidateList candidates = provider.getCandidates(text);

    if (scaleEmojis && candidates != null && candidates.allEmojis) {
      int   emojis = candidates.size();
      float scale  = 1.0f;

      if (emojis <= 8) scale += 0.25f;
      if (emojis <= 6) scale += 0.25f;
      if (emojis <= 4) scale += 0.25f;
      if (emojis <= 2) scale += 0.25f;

      super.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalFontSize * scale);
    } else if (scaleEmojis) {
      super.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalFontSize);
    }

    if (text != null && isSingleLine()) {
      text = text.toString().replace("\r", "\uFEFF")
                            .replace("\n", " ");
    }

    if (unchanged(text, type)) {
      return;
    }

    previousText       = text;
    previousBufferType = type;
    emojiUpdateTime    = getEmojiUpdateTime();
    emojiStyle         = getEmojiStyle();
    useSystemEmoji     = useSystemEmoji();

    if (useSystemEmoji) {
      // Ellipsize correctly on API levels 14-22 (System emoji)
      if (isSingleLine()) super.setHorizontallyScrolling(true);
      super.setText(text, BufferType.NORMAL);
      return;
    }

    // Ellipsize correctly on API level 23+ (Signals emoji)
    if (isSingleLine()) super.setHorizontallyScrolling(false);
    if (useEllipsis(text) && singleLineFix) {
      super.setText(text, BufferType.SPANNABLE);
    } else {
      CharSequence emojified = provider.emojify(candidates, text, this);
      super.setText(emojified, BufferType.SPANNABLE);
    }

    // Android fails to ellipsize spannable strings. (https://issuetracker.google.com/issues/36991688)
    // We ellipsize them ourselves by manually truncating the appropriate section.
    if (getEllipsize() == TextUtils.TruncateAt.END) {
      ellipsize();
    }
  }

  private void ellipsize() {
    post(() -> {
      if (getLayout() == null) {
        ellipsize();
        return;
      }

      if (maxLines <= 0) {
        return;
      }

      int lineCount = getLineCount();
      if (lineCount > maxLines) {
        int overflowStart       = getLayout().getLineStart(maxLines - 1);
        CharSequence overflow   = getText().subSequence(overflowStart, getText().length());
        CharSequence ellipsized = TextUtils.ellipsize(overflow, getPaint(), getWidth(), TextUtils.TruncateAt.END);

        SpannableStringBuilder newContent = new SpannableStringBuilder();
        newContent.append(getText().subSequence(0, overflowStart))
                  .append(ellipsized.subSequence(0, ellipsized.length()));

        EmojiParser.CandidateList newCandidates = EmojiProvider.getInstance(getContext()).getCandidates(newContent);
        CharSequence              emojified     = EmojiProvider.getInstance(getContext()).emojify(newCandidates, newContent, this);

        super.setText(emojified, BufferType.SPANNABLE);
      }
    });
  }

  private boolean useEllipsis(CharSequence text) {
    if (text != null && getEllipsize() == TextUtils.TruncateAt.END) {
      CharSequence overflow   = text.subSequence(0, text.length());
      CharSequence ellipsized = TextUtils.ellipsize(overflow, getPaint(), getWidth(), TextUtils.TruncateAt.END);
      return !Util.equals(text, ellipsized);
    }
    return false;
  }

  private boolean unchanged(CharSequence text, BufferType bufferType) {
    return Util.equals(previousText, text)             &&
           Util.equals(previousBufferType, bufferType) &&
           emojiUpdateTime == getEmojiUpdateTime()     &&
           emojiStyle == getEmojiStyle()               &&
           useSystemEmoji == useSystemEmoji()          &&
           !sizeChangeInProgress;
  }

  private boolean isSingleLine() {
    return maxLines == 1 && getEllipsize() == TextUtils.TruncateAt.END;
  }

  private boolean useSystemEmoji() {
    return TextSecurePreferences.isSystemEmojiPreferred(getContext());
  }

  private int getEmojiStyle() {
    return TextSecurePreferences.getEmojiStyle(getContext());
  }

  private long getEmojiUpdateTime() {
    return TextSecurePreferences.getEmojiLastUpdateTime(getContext());
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if (!sizeChangeInProgress) {
      sizeChangeInProgress = true;
      setText(previousText, previousBufferType);
      sizeChangeInProgress = false;
    }
  }

  @Override
  public void invalidateDrawable(@NonNull Drawable drawable) {
    if (drawable instanceof EmojiDrawable) invalidate();
    else                                   super.invalidateDrawable(drawable);
  }

  @Override
  public void setTextSize(float size) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
  }

  @Override
  public void setTextSize(int unit, float size) {
    this.originalFontSize = TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics());
    super.setTextSize(unit, size);
  }
}
