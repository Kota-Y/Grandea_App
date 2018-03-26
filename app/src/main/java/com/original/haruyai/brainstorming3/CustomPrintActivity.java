package com.original.haruyai.brainstorming3;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.Page;
import android.graphics.pdf.PdfDocument.PageInfo;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomPrintActivity extends AppCompatActivity {
    private int pageHeight;
    private int pageWidth;
    public PdfDocument myPdfDocument;
    public int totalpages = 2;

    String theme = "";
    String date = "";

    List<String> minutes = new ArrayList<>();
    List<String> summary = new ArrayList<>();
    List<String> keywords = new ArrayList<>();
    List<String> participants = new ArrayList<>();

    private String minutes_string;
    private String summary_string;
    private String keywords_string;
    private String participants_string;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_print);

        Intent intent = getIntent();

        //受け取りは配列じゃなくてシンプルな文字列で
        minutes_string = intent.getStringExtra("minutes");
        summary_string = intent.getStringExtra("summary");
        keywords_string = intent.getStringExtra("keywords");
        participants_string = intent.getStringExtra("participants");
        theme = intent.getStringExtra("theme");
        date = intent.getStringExtra("date");

        //文字列、区切りたい文字、格納したい配列
        splitNset(minutes_string, "\n", minutes);
        splitNset(summary_string, "\n", summary);
        splitNset(keywords_string, " ", keywords);
        splitNset(participants_string, ", ", participants);

    }

    //文字列と分割したい文字、格納したいリストを入れればいい感じに帰ってくる関数
    private void splitNset(String string, String split, List<String> list){
        String[] hairetsu = string.split(split);
        for(int s=0; s<hairetsu.length; s++){
            list.add(hairetsu[s]);
        }
    }

    public void printDocument(View view) {
        PrintManager printManager = (PrintManager) this
                .getSystemService(Context.PRINT_SERVICE);

        String jobName = this.getString(R.string.app_name) +
                " Document";

        assert printManager != null;
        printManager.print(jobName, new MyPrintDocumentAdapter(this),
                null);
    }

    public class MyPrintDocumentAdapter extends PrintDocumentAdapter {
        Context context;

        MyPrintDocumentAdapter(Context context) {
            this.context = context;
        }

        private boolean pageInRange(PageRange[] pageRanges, int page) {
            for (PageRange pageRange : pageRanges) {
                if ((page >= pageRange.getStart()) && (page <= pageRange.getEnd()))
                    return true;
            }
            return false;
        }


        @Override
        public void onLayout(PrintAttributes oldAttributes,
                             PrintAttributes newAttributes,
                             CancellationSignal cancellationSignal,
                             LayoutResultCallback callback,
                             Bundle metadata) {
            myPdfDocument = new PrintedPdfDocument(context, newAttributes);

            pageHeight = newAttributes.getMediaSize().getHeightMils() / 1000 * 72;
            pageWidth = newAttributes.getMediaSize().getWidthMils() / 1000 * 72;

            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }

            if (totalpages > 0) {
                PrintDocumentInfo.Builder builder = new PrintDocumentInfo
                        .Builder(date + ".pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(totalpages);

                PrintDocumentInfo info = builder.build();
                callback.onLayoutFinished(info, true);
            } else {
                callback.onLayoutFailed("Page count is zero.");
            }
        }

        @Override
        public void onWrite(final PageRange[] pageRanges,
                            final ParcelFileDescriptor destination,
                            final CancellationSignal cancellationSignal,
                            final WriteResultCallback callback) {

            PageInfo newPage = new PageInfo.Builder(pageWidth, pageHeight, 0).create();
            PdfDocument.Page page = myPdfDocument.startPage(newPage);

            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                myPdfDocument.close();
                myPdfDocument = null;
                return;
            }
            Canvas canvas = page.getCanvas();
            PageInfo pageInfo = page.getInfo();
            TextPaint textPaint = new TextPaint();

            int pageWidth = pageInfo.getPageWidth();
            int pageHeight = pageInfo.getPageHeight();

            int baseLine = 72;
            int xPos, yPos;

            // theme
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(36);
            textPaint.setTextAlign(Align.CENTER);
            xPos = pageWidth / 2;
            yPos = baseLine;
            canvas.drawText(theme, xPos, yPos, textPaint);

            // date
            textPaint.setTextSize(20);
            textPaint.setTextAlign(Align.RIGHT);
            xPos = pageWidth - 15;
            yPos = baseLine + 30;
            canvas.drawText(date, xPos, yPos, textPaint);

            // participants
            xPos = pageWidth - 15;
            yPos = baseLine + 60;
            //canvas.drawText(String.join(", ", participants), xPos, yPos, textPaint);
            //上の文の書き換え
            String join1 = forStringFor(participants, ", ");
            canvas.drawText(join1, xPos, yPos, textPaint);


            // keywords
            textPaint.setColor(Color.RED);
            //textPaint.setTextAlign(Align.CENTER);
            textPaint.setTextAlign(Align.LEFT);
            //xPos = pageWidth / 2;
            xPos = 20;
            yPos = baseLine + 100;
            //canvas.drawText("キーワード: ▶" + String.join("   ▶", keywords), xPos, yPos, textPaint);
            canvas.drawText("キーワード:", xPos, yPos, textPaint);
            canvas.drawText("  ▶   " + keywords.get(0), xPos, yPos+30, textPaint);
            canvas.drawText("  ▶   " + keywords.get(1), xPos, yPos+60, textPaint);
            canvas.drawText("  ▶   " + keywords.get(2), xPos, yPos+90, textPaint);

            // summary + minutes
            textPaint.setColor(Color.BLACK);
            textPaint.setTextAlign(Align.LEFT);
            yPos = baseLine + 200; //120;


            //String.joinを省くため
            String join2 = forStringFor(summary, "\n ◉  ");
            String join3 = forStringFor(minutes, "\n\n  ");

            String summaryStr = "＿＿＿＿＿＿＿＿＿＿＿＿＿\n要約：\n ◉  " + join2;
            String textStr = "内容：\n" + join3;
            String content = summaryStr +
                    "\n＿＿＿＿＿＿＿＿＿＿＿＿＿\n" + textStr;



            RectF rect = new RectF(20, yPos, pageWidth - 20, pageHeight - 30);
            canvas.save();
            canvas.translate(rect.left, rect.top);

            StaticLayout sl = new StaticLayout(content,
                    textPaint, pageWidth, Layout.Alignment.ALIGN_NORMAL,
                    1, 1, false);
            /*StaticLayout.Builder builder = StaticLayout.Builder.obtain(content,
                    0, content.length(), textPaint, pageWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(1, 1)
                    .setIncludePad(true)
                    .setMaxLines(10);
                    */
            //StaticLayout sl = builder.build();
            sl.draw(canvas);
            canvas.restore();

            myPdfDocument.finishPage(page);


            int endLine = 21;
            if (sl.getLineCount() > endLine) {
                newPage = new PageInfo.Builder(pageWidth, pageHeight, 0).create();
                page = myPdfDocument.startPage(newPage);

                content = content.substring(sl.getLineStart(endLine));
                canvas = page.getCanvas();

                textPaint = new TextPaint();
                textPaint.setTextSize(20);
                rect = new RectF(20, 50, pageWidth - 20, pageHeight);
                canvas.save();
                canvas.translate(rect.left, rect.top);
                sl = new StaticLayout(content,
                        textPaint, pageWidth, Layout.Alignment.ALIGN_NORMAL,
                        1, 1, false);
                sl.draw(canvas);
                canvas.restore();

                myPdfDocument.finishPage(page);
            }

            try {
                myPdfDocument.writeTo(new FileOutputStream(
                        destination.getFileDescriptor()));
            } catch (IOException e) {
                callback.onWriteFailed(e.toString());
                return;
            } finally {
                myPdfDocument.close();
                myPdfDocument = null;
            }

            callback.onWriteFinished(pageRanges);
        }

        //イーファンのコードを文字列に対応させること
        private String forStringFor(List<String> list, String separate){
            String return_string = "";
            int loop = list.size();
            for(int r=0; r<loop; r++){
                return_string += list.get(r) + separate;
            }
            return_string = return_string.substring(0, return_string.length()-separate.length());

            return return_string;
        }
    }
}
