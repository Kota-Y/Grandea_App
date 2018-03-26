package com.original.haruyai.brainstorming3;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by haruya.i on 2017/10/30.
 */

public class Summary extends AppCompatActivity{
    private String minutes_string;
    private String summary_string;
    private String keywords_string;
    private String participants_string;
    private String theme_string;
    private String date_string;

    List<String> minutes = new ArrayList<>();
    List<String> summary = new ArrayList<>();
    List<String> keywords = new ArrayList<>();
    List<String> participants = new ArrayList<>();

    private Button pdfButton;

    //summaryとkeywordsは編集してから表示しているから、PDF保存用に分ける
    private String ad_summary;
    private String ad_keywords;

    private TextView tv4string, tv4summary, tv4keywords, tv4theme, tv4date, tv4participants;

    //閲覧用のときの変数
    private String clickedID;
    private boolean viewMode;

    //アイコンを非表示にするため
    MenuItem item;

    //PDF作成で使う
    private int pageHeight;
    private int pageWidth;
    public PdfDocument myPdfDocument;

    //「議事録」って表示されてるとこ
    TextView gijiroku;

    //ルイスが作ってくれた日英翻訳部分---------------------------------------------------------------------------
    boolean isJapanese = true;

    public void onEigoButtonClicked(View view){
        try {
            if (isJapanese){
                tv4string.setText(new getTranslation().execute(tv4string.getText().toString()).get());
                tv4summary.setText("Summary:\n   " + new getTranslation().execute(tv4summary.getText().toString()).get());
                tv4keywords.setText("Keywords:\n   " + new getTranslation().execute(tv4keywords.getText().toString()).get());
                tv4theme.setText(new getTranslation().execute(tv4theme.getText().toString()).get());
                tv4date.setText(new getTranslation().execute(tv4date.getText().toString()).get());
                tv4participants.setText(new getTranslation().execute(tv4participants.getText().toString()).get());
                isJapanese = false;
                ((Button)(view)).setText("日本語");

                gijiroku.setText("Minutes");
            }
            else{
                tv4string.setText(minutes_string);
                tv4summary.setText(ad_summary);
                tv4keywords.setText(ad_keywords);
                tv4theme.setText(theme_string);
                tv4date.setText(date_string);
                tv4participants.setText(participants_string);
                isJapanese = true;
                ((Button)(view)).setText("英語");

                gijiroku.setText("議事録");
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private class getTranslation extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... text) {
            try {
                String uRL = "http://fullfill.sakura.ne.jp/JPHACKS/translate/translate.php";
                HttpClient httpclient = new DefaultHttpClient(null);
                HttpPost httppost = new HttpPost(uRL);

                // Request parameters and other properties.
                List<NameValuePair> params = new ArrayList<NameValuePair>(1);
                params.add(new BasicNameValuePair("text", text[0]));
                httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

                //Execute and get the response.
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity);
            } catch (Exception e) {
                Log.e("log_tag", "Error converting result " + e.toString());
            }
            return "";
        }
    }
    //ルイスが作ってくれた日英翻訳部分---------------------------------------------------------------------------

    //ここでactionbar_menu.xmlをActionBar上に表示している
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu_minutes, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        item = menu.findItem(R.id.bubble_icon);
        //閲覧モードのときバブルアイコンを表示する. onPrepareOptionsMenuで定義している
        if(viewMode==true){
            item.setVisible(true);
        }else{
            item.setVisible(false);
        }

        return true;
    }

    //ActionBar上のアイコン（Item）が押されたことを検知するための関数
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //IDごとにボタンを押した際の処理を記述
        switch (item.getItemId()) {
            //戻るアイコンが押された時
            case android.R.id.home:
                finish();
                return true;

            //バブルアイコンが押された時
            case R.id.bubble_icon:
                Intent intent2MA = new Intent(getApplication(), MainActivity.class);

                intent2MA.putExtra("clickedID", clickedID);
                intent2MA.putExtra("viewMode", viewMode);

                startActivity(intent2MA);

                break;

            /*
            //shareボタンが押された時
            case R.id.share:
                /*
                Intent intent2CP = new Intent(getApplication(), CustomPrintActivity.class);


                intent2CP.putExtra("minutes", minutes_string);
                intent2CP.putExtra("summary", summary_string);
                intent2CP.putExtra("keywords", keywords_string);
                intent2CP.putExtra("participants", participants_string);
                intent2CP.putExtra("theme", theme_string);
                intent2CP.putExtra("date", date_string);



                startActivity(intent2CP);

                */
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.summary_layout);

        Intent intent = getIntent();

        //受け取りは配列じゃなくてシンプルな文字列で
        minutes_string = intent.getStringExtra("minutes");
        summary_string = intent.getStringExtra("summary");
        keywords_string = intent.getStringExtra("keywords");
        participants_string = intent.getStringExtra("participants");
        theme_string = intent.getStringExtra("theme");
        date_string = intent.getStringExtra("date");

        //閲覧用の時
        clickedID = intent.getStringExtra("clickedID");
        viewMode = intent.getBooleanExtra("viewMode", false);

        tv4string = (TextView)this.findViewById(R.id.string_line);
        tv4summary = (TextView)this.findViewById(R.id.summary_line);
        tv4keywords = (TextView)this.findViewById(R.id.keywords_line);
        pdfButton = (Button)this.findViewById(R.id.pdfButton);
        tv4theme = (TextView)this.findViewById(R.id.theme_line);
        tv4date = (TextView)this.findViewById(R.id.date_line);
        tv4participants = (TextView)this.findViewById(R.id.participants_line);

        gijiroku = (TextView)this.findViewById(R.id.textView);

        //頭にくっつける
        ad_summary = "要約：\n" + summary_string;
        ad_keywords = "キーワード：\n" + keywords_string;

        //適切な箇所に表示
        tv4string.setText(minutes_string);
        tv4summary.setText(ad_summary);
        tv4keywords.setText(ad_keywords);
        tv4theme.setText(theme_string);
        tv4date.setText(date_string);
        tv4participants.setText(participants_string);

        // ActionBarの設定
        if (savedInstanceState == null) {
            // customActionBarの取得
            View customActionBarView = this.getActionBarView("Hoge", "表示する画像のURL");
            // ActionBarの取得
            android.support.v7.app.ActionBar actionBar = this.getSupportActionBar();
            // 戻るボタンを表示するかどうか('<' <- こんなやつ)
            actionBar.setDisplayHomeAsUpEnabled(true);
            // タイトルを表示するか（もちろん表示しない）
            actionBar.setDisplayShowTitleEnabled(false);
            // iconを表示するか（もちろん表示しない）
            actionBar.setDisplayShowHomeEnabled(false);
            // ActionBarにcustomViewを設定する
            actionBar.setCustomView(customActionBarView);
            // CustomViewを表示するか
            actionBar.setDisplayShowCustomEnabled(true);
        }

        //文字列、区切りたい文字、格納したい配列
        splitNset(minutes_string, "\n", minutes);
        splitNset(summary_string, "\n", summary);
        splitNset(keywords_string, " ", keywords);
        splitNset(participants_string, ", ", participants);

        //PDF共有用
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

    }

    //文字列と分割したい文字、格納したいリストを入れればいい感じに帰ってくる関数
    private void splitNset(String string, String split, List<String> list){
        String[] hairetsu = string.split(split);
        for(int s=0; s<hairetsu.length; s++){
            list.add(hairetsu[s]);
        }
    }

    //PDFボタンが押されと時に呼ばれるかんす
    public void printDocument(View view) {
        PrintManager printManager = (PrintManager) this
                .getSystemService(Context.PRINT_SERVICE);

        String jobName = this.getString(R.string.app_name) +
                " Document";

        assert printManager != null;
        printManager.print(jobName, new MyPrintDocumentAdapter(this),
                null);
    }

    //shareボタンが押された時に呼ばれる関数. ViewをMenuItemに書き換えてる
    public void share(MenuItem menuItem) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        System.out.println(Environment.getExternalStorageDirectory().getPath());
        File file = new File(Environment.getExternalStorageDirectory().getPath()
                + "/Download/" + theme_string + ".pdf");
        System.out.println(file.exists());
        System.out.println(file.getName());
        System.out.println(file.length());
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        startActivity(Intent.createChooser(shareIntent, "Share pdf using"));
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

            PrintDocumentInfo.Builder builder = new PrintDocumentInfo
                    .Builder(theme_string + ".pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT);

            PrintDocumentInfo info = builder.build();
            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(final PageRange[] pageRanges,
                            final ParcelFileDescriptor destination,
                            final CancellationSignal cancellationSignal,
                            final WriteResultCallback callback) {

            PdfDocument.PageInfo newPage = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 0).create();
            PdfDocument.Page page = myPdfDocument.startPage(newPage);

            if (cancellationSignal.isCanceled()) {
                callback.onWriteCancelled();
                myPdfDocument.close();
                myPdfDocument = null;
                return;
            }
            Canvas canvas = page.getCanvas();
            PdfDocument.PageInfo pageInfo = page.getInfo();
            TextPaint textPaint = new TextPaint();

            int pageWidth = pageInfo.getPageWidth();
            int pageHeight = pageInfo.getPageHeight();

            int baseLine = 72;
            int xPos, yPos;

            // theme
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(36);
            textPaint.setTextAlign(Paint.Align.CENTER);
            xPos = pageWidth / 2;
            yPos = baseLine;
            canvas.drawText(theme_string, xPos, yPos, textPaint);

            // date
            textPaint.setTextSize(20);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            xPos = pageWidth - 15;
            yPos = baseLine + 30;
            canvas.drawText(date_string, xPos, yPos, textPaint);

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
            textPaint.setTextAlign(Paint.Align.LEFT);
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
            textPaint.setTextAlign(Paint.Align.LEFT);
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
            while (sl.getLineCount() > endLine) {
                newPage = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 0).create();
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

                endLine = 30;
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

    public View getActionBarView(String title, String imageURL) {
        // 表示するlayoutファイルの取得
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.custom_action_bar, null);

        // CustomViewにクリックイベントを登録する
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        return view;
    }
}
