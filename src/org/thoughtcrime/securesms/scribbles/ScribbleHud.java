package org.thoughtcrime.securesms.scribbles;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.scribbles.widget.VerticalSlideColorPicker;

/**
 * The HUD (heads-up display) that contains all of the tools for interacting with
 * {@link org.thoughtcrime.securesms.scribbles.widget.ScribbleView}
 */
public class ScribbleHud extends FrameLayout implements VerticalSlideColorPicker.OnColorChangeListener {

  private View                     drawButton;
  private View                     textButton;
  private View                     stickerButton;
  private View                     undoButton;
  private View                     deleteButton;
  private View                     saveButton;
  private VerticalSlideColorPicker colorPicker;

  private EventListener eventListener;
  private int           activeColor;

  public ScribbleHud(@NonNull Context context) {
    super(context);
    initialize();
  }

  public ScribbleHud(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public ScribbleHud(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  private void initialize() {
    inflate(getContext(), R.layout.scribble_hud, this);

    drawButton    = findViewById(R.id.scribble_draw_button);
    textButton    = findViewById(R.id.scribble_text_button);
    stickerButton = findViewById(R.id.scribble_sticker_button);
    undoButton    = findViewById(R.id.scribble_undo_button);
    deleteButton  = findViewById(R.id.scribble_delete_button);
    saveButton    = findViewById(R.id.scribble_save_button);
    colorPicker   = findViewById(R.id.scribble_color_picker);

    colorPicker.setOnColorChangeListener(this);

    undoButton.setOnClickListener(v -> {
      if (eventListener != null) {
        eventListener.onUndo();
      }
    });

    deleteButton.setOnClickListener(v -> {
      if (eventListener != null) {
        eventListener.onDelete();
      }
      setMode(Mode.NONE);
    });

    saveButton.setOnClickListener(v -> {
      if (eventListener != null) {
        eventListener.onSave();
      }
      setMode(Mode.NONE);
    });

    setMode(Mode.NONE);
  }

  public void enterMode(@NonNull Mode mode) {
    setMode(mode, false);
  }

  private void setMode(@NonNull Mode mode) {
    setMode(mode, true);
  }

  private void setMode(@NonNull Mode mode, boolean notify) {
    switch (mode) {
      case NONE:    presentModeNone(); break;
      case DRAW:    presentModeDraw(); break;
      case TEXT:    presentModeText(); break;
      case STICKER: presentModeSticker(); break;
    }

    if (notify && eventListener != null) {
      eventListener.onModeStarted(mode);
    }
  }

  private void presentModeNone() {
    drawButton.setVisibility(VISIBLE);
    textButton.setVisibility(VISIBLE);
    stickerButton.setVisibility(VISIBLE);

    undoButton.setVisibility(GONE);
    deleteButton.setVisibility(GONE);
    colorPicker.setVisibility(GONE);

    drawButton.setOnClickListener(v -> setMode(Mode.DRAW));
    textButton.setOnClickListener(v -> setMode(Mode.TEXT));
    stickerButton.setOnClickListener(v -> setMode(Mode.STICKER));
  }

  private void presentModeDraw() {
    drawButton.setVisibility(VISIBLE);
    undoButton.setVisibility(VISIBLE);
    colorPicker.setVisibility(VISIBLE);

    textButton.setVisibility(GONE);
    stickerButton.setVisibility(GONE);
    deleteButton.setVisibility(GONE);

    drawButton.setOnClickListener(v -> setMode(Mode.NONE));
  }

  private void presentModeText() {
    textButton.setVisibility(VISIBLE);
    deleteButton.setVisibility(VISIBLE);
    colorPicker.setVisibility(VISIBLE);

    drawButton.setVisibility(GONE);
    stickerButton.setVisibility(GONE);
    undoButton.setVisibility(GONE);

    textButton.setOnClickListener(v -> setMode(Mode.NONE));
  }

  private void presentModeSticker() {
    stickerButton.setVisibility(VISIBLE);
    deleteButton.setVisibility(VISIBLE);

    drawButton.setVisibility(GONE);
    textButton.setVisibility(GONE);
    undoButton.setVisibility(GONE);
    colorPicker.setVisibility(GONE);

    stickerButton.setOnClickListener(v -> setMode(Mode.NONE));
  }

  @Override
  public void onColorChange(int selectedColor) {
    activeColor = selectedColor;

    if (eventListener != null) {
      eventListener.onColorChange(selectedColor);
    }
  }

  public int getActiveColor() {
    return activeColor;
  }

  public void setEventListener(@Nullable EventListener eventListener) {
    this.eventListener = eventListener;
  }

  public enum Mode {
    NONE, DRAW, TEXT, STICKER
  }

  public interface EventListener {
    void onModeStarted(@NonNull Mode mode);
    void onColorChange(int color);
    void onUndo();
    void onDelete();
    void onSave();
  }
}
