package com.quran.labs.androidquran.ui.translation;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.QuranAyah;
import com.quran.labs.androidquran.data.QuranInfo;
import com.quran.labs.androidquran.model.translation.ArabicDatabaseUtils;
import com.quran.labs.androidquran.ui.helpers.UthmaniSpan;
import com.quran.labs.androidquran.util.QuranSettings;
import com.quran.labs.androidquran.widgets.AyahNumberView;
import com.quran.labs.androidquran.widgets.DividerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

class TranslationAdapter extends RecyclerView.Adapter<TranslationAdapter.RowViewHolder> {
  private static final boolean USE_UTHMANI_SPAN =
      Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1;
  private static final float ARABIC_MULTIPLIER = 1.4f;

  private final Context context;
  private final LayoutInflater inflater;
  private final List<TranslationViewRow> data;
  private View.OnClickListener onClickListener;

  private int fontSize;
  private int textColor;
  private int dividerColor;
  private int arabicTextColor;
  private int suraHeaderColor;
  private int ayahSelectionColor;
  private boolean isNightMode;

  private int highlightedAyah;
  private int highlightedRowCount;
  private int highlightedStartPosition;

  private View.OnClickListener defaultClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (onClickListener != null) {
        onClickListener.onClick(v);
      }
    }
  };

  TranslationAdapter(Context context) {
    this.context = context;
    this.data = new ArrayList<>();
    this.inflater = LayoutInflater.from(context);
  }

  void setData(List<TranslationViewRow> data) {
    this.data.clear();
    this.data.addAll(data);
    if (highlightedAyah > 0) {
      highlightAyah(highlightedAyah, false);
    }
  }

  void setHighlightedAyah(int ayahId) {
    highlightAyah(ayahId, true);
  }

  private void highlightAyah(int ayahId, boolean notify) {
    if (ayahId != highlightedAyah) {
      int count = 0;
      int startPosition = -1;
      for (int i = 0, size = this.data.size(); i < size; i++) {
        QuranAyah item = this.data.get(i).data;
        if (item.getAyahId() == ayahId) {
          if (count == 0) {
            startPosition = i;
          }
          count++;
        } else if (count > 0) {
          break;
        }
      }

      highlightedAyah = ayahId;
      if (highlightedRowCount > 0 && notify) {
        notifyItemRangeChanged(highlightedStartPosition, highlightedRowCount);
      }

      highlightedStartPosition = startPosition;
      highlightedRowCount = count;
      if (count > 0 && notify) {
        notifyItemRangeChanged(startPosition, count);
      }
    }
  }

  void unhighlight() {
    if (highlightedAyah > 0 && highlightedRowCount > 0) {
      notifyItemRangeChanged(highlightedAyah, highlightedRowCount);
    }

    highlightedAyah = 0;
    highlightedRowCount = 0;
    highlightedStartPosition = -1;
  }

  void setOnTranslationClickedListener(View.OnClickListener listener) {
    this.onClickListener = listener;
  }

  void refresh(QuranSettings quranSettings) {
    this.fontSize = quranSettings.getTranslationTextSize();
    isNightMode = quranSettings.isNightMode();
    if (isNightMode) {
      int textBrightness = quranSettings.getNightModeTextBrightness();
      this.textColor = Color.rgb(textBrightness, textBrightness, textBrightness);
      this.arabicTextColor = textColor;
      this.dividerColor = textColor;
      this.suraHeaderColor = ContextCompat.getColor(context, R.color.translation_sura_header_night);
      this.ayahSelectionColor =
          ContextCompat.getColor(context, R.color.translation_ayah_selected_color_night);
    } else {
      this.textColor = ContextCompat.getColor(context, R.color.translation_text_color);
      this.dividerColor = ContextCompat.getColor(context, R.color.translation_divider_color);
      this.arabicTextColor = Color.BLACK;
      this.suraHeaderColor = ContextCompat.getColor(context, R.color.translation_sura_header);
      this.ayahSelectionColor =
          ContextCompat.getColor(context, R.color.translation_ayah_selected_color);
    }

    if (!this.data.isEmpty()) {
      notifyDataSetChanged();
    }
  }

  @Override
  public int getItemViewType(int position) {
    return data.get(position).type;
  }

  @Override
  public RowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    @LayoutRes int layout;
    if (viewType == TranslationViewRow.Type.SURA_HEADER) {
      layout = R.layout.quran_translation_header_row;
    } else if (viewType == TranslationViewRow.Type.BASMALLAH ||
        viewType == TranslationViewRow.Type.QURAN_TEXT) {
      layout = R.layout.quran_translation_arabic_row;
    } else if (viewType == TranslationViewRow.Type.SPACER) {
      layout = R.layout.quran_translation_spacer_row;
    } else if (viewType == TranslationViewRow.Type.VERSE_NUMBER) {
      layout = R.layout.quran_translation_verse_number_row;
    } else if (viewType == TranslationViewRow.Type.TRANSLATOR) {
      layout = R.layout.quran_translation_translator_row;
    } else {
      layout = R.layout.quran_translation_text_row;
    }
    View view = inflater.inflate(layout, parent, false);
    return new RowViewHolder(view);
  }

  @Override
  public void onBindViewHolder(RowViewHolder holder, int position) {
    TranslationViewRow row = data.get(position);

    if (holder.text != null) {
      final CharSequence text;
      if (row.type == TranslationViewRow.Type.SURA_HEADER) {
        text = QuranInfo.getSuraName(context, row.data.getSura(), true);
        holder.text.setBackgroundColor(suraHeaderColor);
      } else if (row.type == TranslationViewRow.Type.BASMALLAH ||
          row.type == TranslationViewRow.Type.QURAN_TEXT) {
        SpannableString str = new SpannableString(row.type == TranslationViewRow.Type.BASMALLAH ?
            ArabicDatabaseUtils.AR_BASMALLAH : ArabicDatabaseUtils.getAyahWithoutBasmallah(
            row.data.getSura(), row.data.getAyah(), row.data.getText()));
        if (USE_UTHMANI_SPAN) {
          str.setSpan(new UthmaniSpan(context), 0, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        text = str;
        holder.text.setTextColor(arabicTextColor);
        holder.text.setTextSize(ARABIC_MULTIPLIER * fontSize);
      } else {
        if (row.type == TranslationViewRow.Type.TRANSLATOR) {
          text = row.data.getTranslator();
        } else {
          text = row.data.getTranslation();
          holder.text.setTextColor(textColor);
          holder.text.setTextSize(fontSize);
        }
      }
      holder.text.setText(text);
    } else if (holder.divider != null) {
      boolean showLine = true;
      if (position + 1 < data.size()) {
        TranslationViewRow nextRow = data.get(position + 1);
        if (nextRow.data.getSura() != row.data.getSura()) {
          showLine = false;
        }
      } else {
        showLine = false;
      }
      holder.divider.toggleLine(showLine);
      holder.divider.setDividerColor(dividerColor);
    } else if (holder.ayahNumber != null) {
      String text = context.getString(R.string.sura_ayah, row.data.getSura(), row.data.getAyah());
      holder.ayahNumber.setAyahString(text);
      holder.ayahNumber.setNightMode(isNightMode);
    }

    // toggle highlighting of the ayah, but not for sura headers and basmallah
    boolean isHighlighted = row.data.getAyahId() == highlightedAyah;
    if (row.type != TranslationViewRow.Type.SURA_HEADER &&
        row.type != TranslationViewRow.Type.BASMALLAH &&
        row.type != TranslationViewRow.Type.SPACER) {
      if (isHighlighted) {
        holder.wrapperView.setBackgroundColor(ayahSelectionColor);
      } else {
        holder.wrapperView.setBackgroundColor(0);
      }
    } else if (row.type == TranslationViewRow.Type.SPACER) {
      if (isHighlighted) {
        holder.divider.highlight(ayahSelectionColor);
      } else {
        holder.divider.unhighlight();
      }
    }
  }

  @Override
  public int getItemCount() {
    return data.size();
  }

  class RowViewHolder extends RecyclerView.ViewHolder {
    @NonNull View wrapperView;
    @BindView(R.id.text) @Nullable TextView text;
    @BindView(R.id.divider) @Nullable DividerView divider;
    @BindView(R.id.ayah_number) @Nullable AyahNumberView ayahNumber;

    RowViewHolder(@NonNull View itemView) {
      super(itemView);
      this.wrapperView = itemView;
      ButterKnife.bind(this, itemView);
      itemView.setOnClickListener(defaultClickListener);
    }
  }
}
