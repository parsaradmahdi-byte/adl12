package com.rahedalat.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayDeque;

public class MainActivity extends Activity {
    private GameView game;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(12,16,20));
        getWindow().setNavigationBarColor(Color.rgb(12,16,20));
        game = new GameView(this);
        setContentView(game);
    }

    @Override public void onBackPressed() {
        if (!game.goBack()) game.askExit();
    }

    public static class GameView extends View {
        static final int START=0, OFFICE=1, LIBRARY=2, CASE=3, LESSON=4, QUIZ=5,
                RESULT=6, PROGRESS=7, ARCHIVE=8, LAWBOOK=9,
                CASE_ROOM=10, CASE1_AMIR=11, CASE1_BAG=12, CASE1_EVIDENCE=13, CASE1_PHONE=14, CASE1_RECEIPTS=15, CASE1_DVD=16, CASE1_THINK=17, CASE1_COMPLETE=18;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final ArrayDeque<Integer> history = new ArrayDeque<>();
        final SharedPreferences prefs;

        Bitmap officeBg, libraryBg, lawyer, client, judge, officer;
        Bitmap caseRoomAmir, amirCloseup, amirEvidenceDialogue, bagCloseup, evidenceOpen, roomWorkScroll;
        MediaPlayer music;
        int screen = START;
        int selectedCase = 1;
        int storyStep = 0;
        boolean settings = false, exitConfirm = false, answerChosen = false, correct = false;
        float ox, oy, scale;
        float roomPan = 0f, roomDownX = 0f, roomStartPan = 0f;
        boolean roomDragging = false;
        int case1Dialogue = 0;
        boolean bagUnlocked = false;
        boolean phoneUnlocked = false, phoneClueOpen = false;
        boolean receiptsSolved = false, dvdSolved = false;
        int phoneEntry = 0;
        int draggedReceipt = -1;
        float dragX=0f, dragY=0f;
        int[] receiptSlots = {-1,-1,-1};
        float dvdTime = 0f;
        boolean dvdClueOpen = false;
        int thoughtChoice = -1;

        final String[] titles = {
                "",
                "پرونده قرارداد و امضای مورد اختلاف",
                "پرونده مطالبه وجه و تشخیص طرفین دعوا",
                "پرونده نقص دادخواست و تکمیل اطلاعات",
                "پرونده مطالبه خسارت و تعیین خواسته",
                "پرونده سند و اثبات ادعا"
        };

        final String[] casePeople = {"", "مریم احمدی", "رضا کریمی", "سارا موسوی", "علی نادری", "نگار حسینی"};
        final String[] caseIssues = {
                "",
                "امضای قراردادی که موکل انتساب آن را نمی‌پذیرد.",
                "مطالبه وجهی که در آن باید خواهان و خوانده درست شناسایی شوند.",
                "دادخواستی که بخشی از اطلاعات ضروری آن ناقص ثبت شده است.",
                "مطالبه خسارت و تشخیص دقیق چیزی که از دادگاه درخواست می‌شود.",
                "اختلاف درباره سند و ارزش آن به عنوان دلیل در دعوا."
        };

        final String[][] story = {
                {},
                {"من قراردادی را امضا نکرده‌ام، اما طرف مقابل نسخه‌ای از قرارداد را ارائه کرده است.",
                 "اول باید بدانیم دعوا از چه مسیری وارد دادگاه شده و چه چیزی از دادگاه خواسته شده است.",
                 "اگر امضا را قبول ندارم، باید ادعای خود را روشن و دلیل‌های موجود را بررسی کنم.",
                 "حالا پرونده آماده است تا ببینیم نخستین اقدام حقوقی درست چیست."},
                {"من از شخصی طلب دارم و می‌خواهم مبلغ بدهی را از راه قانونی مطالبه کنم.",
                 "قبل از هر چیز باید مشخص کنیم چه کسی دعوا را مطرح می‌کند و دعوا علیه چه کسی است.",
                 "پس در این پرونده، عنوان خواهان و خوانده فقط یک اسم نیست؛ نقش هر طرف را مشخص می‌کند.",
                 "حالا باید تصمیم بگیریم در دادخواست چه کسی خواهان و چه کسی خوانده است."},
                {"دادخواست ثبت شده، اما بخشی از اطلاعات آن ناقص است و دفتر دادگاه ایراد گرفته است.",
                 "برای ادامه رسیدگی، باید بدانیم دادخواست چه اطلاعاتی لازم دارد و هر بخش چه کاربردی دارد.",
                 "مشخصات طرفین، خواسته و دلایل از اطلاعاتی هستند که باید با دقت بررسی شوند.",
                 "حالا نوبت توست که تشخیص بدهی کدام اطلاعات برای رفع ایراد مهم‌تر است."},
                {"من خسارتی دیده‌ام و می‌خواهم جبران آن را از دادگاه بخواهم.",
                 "خواسته باید روشن باشد؛ دادگاه باید بداند دقیقاً چه چیزی از او درخواست شده است.",
                 "همچنین در موارد لازم، ارزش خواسته می‌تواند در هزینه دادرسی اثر داشته باشد.",
                 "پس قبل از ادامه پرونده باید خواسته را دقیق و قابل فهم بیان کنیم."},
                {"طرف مقابل می‌گوید سندی دارد که ادعای او را ثابت می‌کند، اما من درباره آن اختلاف دارم.",
                 "اینجا باید مفهوم دلیل را بشناسیم؛ هر ادعا برای اثبات خود به دلیل مناسب نیاز دارد.",
                 "سند یکی از ادله است، اما باید جایگاه و ارتباط آن با ادعا را بررسی کرد.",
                 "اکنون باید تشخیص بدهی دلیل در این پرونده چه نقشی دارد."}
        };

        final String[][] lesson = {
                {},
                {"رسیدگی مدنی با طرح دعوا از مسیر قانونی آغاز می‌شود. دادخواست وسیله طرح بسیاری از دعاوی مدنی است و باید اطلاعات لازم را در خود داشته باشد.",
                 "اصطلاح کلیدی: دادخواست — نوشته‌ای که خواهان برای طرح دعوا و آغاز رسیدگی، مطابق مقررات به مرجع صالح ارائه می‌کند."},
                {"خواهان کسی است که دعوا را مطرح می‌کند و خوانده کسی است که دعوا علیه او مطرح شده است. تشخیص این دو نقش برای تنظیم درست پرونده ضروری است.",
                 "اصطلاح کلیدی: خواهان و خوانده — دو عنوان ناظر به نقش طرفین در دعوا، نه صرفاً نام اشخاص."},
                {"دادخواست باید اطلاعات لازم درباره اصحاب دعوا، خواسته و جهات و دلایل مورد استناد را در حدود قانون داشته باشد. نقص اطلاعات می‌تواند روند پرونده را متوقف یا اصلاح‌پذیر کند.",
                 "اصطلاح کلیدی: مشخصات اصحاب دعوا — اطلاعاتی که برای شناسایی و ابلاغ به طرفین لازم است."},
                {"خواسته همان چیزی است که خواهان از دادگاه می‌خواهد. روشن بودن خواسته به دادگاه و طرف مقابل کمک می‌کند حدود دعوا مشخص باشد.",
                 "اصطلاح کلیدی: خواسته — نتیجه یا امری که خواهان صدور حکم یا تصمیم درباره آن را از دادگاه درخواست می‌کند."},
                {"دلیل وسیله اثبات یا تقویت ادعای مطرح‌شده در حدود قانون است. سند، اقرار، شهادت و سایر ادله قانونی می‌توانند در اثبات موضوع نقش داشته باشند.",
                 "اصطلاح کلیدی: دلیل — وسیله‌ای که برای اثبات یا تقویت ادعا یا دفاع در چارچوب قانون ارائه می‌شود."}
        };

        final String[] questions = {
                "",
                "برای شروع یک دعوای مدنی، کدام اقدام در این پرونده نقش اصلی دارد؟",
                "در پرونده رضا، چه کسی عنوان خواهان دارد؟",
                "برای رفع ایراد دادخواست، کدام مورد باید با دقت بررسی شود؟",
                "در پرونده خسارت، خواسته به چه چیزی اشاره می‌کند؟",
                "در پرونده نگار، دلیل چه نقشی دارد؟"
        };

        final String[][] options = {
                {},
                {"تقدیم دادخواست از مسیر قانونی", "تماس تلفنی با طرف مقابل", "ارسال یک پیام غیررسمی"},
                {"شخصی که دادخواست علیه او مطرح شده", "شخصی که مطالبه وجه را مطرح کرده", "قاضی رسیدگی‌کننده"},
                {"رنگ جلد پرونده", "اطلاعات ضروری اصحاب دعوا و خواسته و دلایل", "شماره صندلی دادگاه"},
                {"نام ساختمان دادگستری", "زمان مراجعه به دفتر", "چیزی که خواهان از دادگاه درخواست می‌کند"},
                {"تزئین پرونده", "تعیین رنگ فرم دادخواست", "وسیله اثبات یا تقویت ادعا در چارچوب قانون"}
        };

        final int[] correctIndex = {0,0,1,1,2,2};
        final String[] explanations = {
                "",
                "طرح دعوا از مسیر قانونی و تقدیم دادخواست، نقطه شروع این پرونده است.",
                "در این پرونده رضا که مطالبه وجه را مطرح کرده، خواهان است؛ طرف مقابل خوانده است.",
                "مشخصات اصحاب دعوا، خواسته و دلایل از اطلاعات مهم دادخواست هستند و باید دقیق بررسی شوند.",
                "خواسته همان چیزی است که خواهان از دادگاه درخواست می‌کند؛ در اینجا جبران خسارت.",
                "دلیل برای اثبات یا تقویت ادعا در حدود قانون به کار می‌رود و سند یکی از انواع آن است."
        };

        public GameView(Context c) {
            super(c);
            setFocusable(true);
            prefs = c.getSharedPreferences("rah_edalat", Context.MODE_PRIVATE);
            officeBg = load(R.drawable.office_bg);
            libraryBg = load(R.drawable.library_bg);
            lawyer = load(R.drawable.char_lawyer);
            client = load(R.drawable.char_client);
            judge = load(R.drawable.char_judge);
            officer = load(R.drawable.char_officer);
            caseRoomAmir = load(R.drawable.case_room_amir);
            amirCloseup = load(R.drawable.case1_amir_closeup);
            amirEvidenceDialogue = load(R.drawable.case1_amir_evidence_dialogue);
            bagCloseup = load(R.drawable.case1_bag_closeup_password);
            evidenceOpen = load(R.drawable.case1_evidence_open);
            roomWorkScroll = load(R.drawable.room_work_scroll_bg);
            tp.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        }

        Bitmap load(int id) { return BitmapFactory.decodeResource(getResources(), id); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight();
            scale=Math.min(w/1080f,h/2400f);
            ox=(w-1080f*scale)/2f; oy=(h-2400f*scale)/2f;
            c.drawColor(Color.rgb(9,13,17));
            c.save(); c.translate(ox,oy); c.scale(scale,scale);
            if(screen==START) drawStart(c); else {
                drawBackground(c);
                if(screen==OFFICE) drawOffice(c);
                else if(screen==LIBRARY) drawLibrary(c);
                else if(screen==CASE) drawCase(c);
                else if(screen==LESSON) drawLesson(c);
                else if(screen==QUIZ) drawQuiz(c);
                else if(screen==RESULT) drawResult(c);
                else if(screen==PROGRESS) drawProgress(c);
                else if(screen==ARCHIVE) drawArchive(c);
                else if(screen==LAWBOOK) drawLawBook(c);
                else if(screen==CASE_ROOM) drawCaseRoom(c);
                else if(screen==CASE1_AMIR) drawCase1Amir(c);
                else if(screen==CASE1_BAG) drawCase1Bag(c);
                else if(screen==CASE1_EVIDENCE) drawCase1Evidence(c);
                else if(screen==CASE1_PHONE) drawCase1Phone(c);
                else if(screen==CASE1_RECEIPTS) drawCase1EvidenceDetail(c, 1);
                else if(screen==CASE1_DVD) drawCase1Dvd(c);
                else if(screen==CASE1_THINK) drawCase1Think(c);
                else if(screen==CASE1_COMPLETE) drawCase1Complete(c);
                drawTop(c);
            }
            if(settings) drawSettings(c);
            if(exitConfirm) drawExit(c);
            c.restore();
        }

        void drawBackground(Canvas c) {
            if(screen==CASE_ROOM || screen==CASE1_AMIR || screen==CASE1_BAG) return;
            Bitmap b = screen==LIBRARY ? libraryBg : officeBg;
            if(b!=null) c.drawBitmap(b,null,new RectF(0,0,1080,2400),p);
            p.setColor(0x18000000); c.drawRect(0,0,1080,2400,p);
        }

        void drawStart(Canvas c) {
            p.setColor(0xff0d151b); c.drawRect(0,0,1080,2400,p);
            if(officeBg!=null){p.setAlpha(70);c.drawBitmap(officeBg,null,new RectF(0,0,1080,2400),p);p.setAlpha(255);}
            p.setColor(0x990a1116); c.drawRect(0,0,1080,2400,p);
            text(c,"راه عدالت",540,700,82,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,900);
            text(c,"یادگیری آیین دادرسی مدنی در قالب پرونده و تصمیم",540,835,34,0xffeadfcd,true,Layout.Alignment.ALIGN_CENTER,900);
            round(c,300,1030,780,1260,34,0xff17435d);
            text(c,"ورود به دفتر وکیل",540,1175,44,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,520);
            text(c,"ساخته شده توسط پارسا",540,2120,30,0xffd1c2ab,true,Layout.Alignment.ALIGN_CENTER,650);
        }

        void drawTop(Canvas c) {
            p.setColor(0x9d101a21); c.drawRect(0,0,1080,145,p);
            if(screen!=OFFICE && screen!=START){
                round(c,34,28,154,124,46,0xdd17313f);
                text(c,"‹",94,99,62,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,90);
            }
            round(c,926,28,1046,124,46,0xdd17313f);
            text(c,"⚙",986,98,40,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,90);
            text(c,screenTitle(),540,91,40,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
        }

        String screenTitle(){
            switch(screen){
                case OFFICE:return "دفتر وکیل";
                case LIBRARY:return "کتابخانه پرونده‌ها";
                case CASE:return "پرونده جاری";
                case LESSON:return "یادگیری پرونده";
                case QUIZ:return "تصمیم حقوقی";
                case RESULT:return "نتیجه پرونده";
                case PROGRESS:return "پیشرفت";
                case ARCHIVE:return "بایگانی";
                case LAWBOOK:return "کتاب قانون";
                case CASE_ROOM:return "اتاق رسیدگی پرونده";
                case CASE1_AMIR:return "گفت‌وگو با امیر";
                case CASE1_BAG:return "بررسی کیف";
                case CASE1_EVIDENCE:return "مدارک داخل کیف";
                case CASE1_PHONE:return "گوشی همراه امیر";
                case CASE1_RECEIPTS:return "رسیدهای پاره‌شده";
                case CASE1_DVD:return "فیلم دوربین مداربسته";
                case CASE1_THINK:return "باکس تفکر وکیل";
                case CASE1_COMPLETE:return "نتیجه بررسی پرونده";
                default:return "راه عدالت";
            }
        }

        void drawOffice(Canvas c) {
            // No visible buttons or labels are placed over office objects.
            // The real objects in the background are the touch targets.
            text(c,"برای تعامل، خودِ اشیای دفتر را لمس کن",540,2310,26,0xfff0e4cf,true,Layout.Alignment.ALIGN_CENTER,850);
        }

        void drawLibrary(Canvas c) {
            text(c,"برای باز کردن پرونده، خودِ پوشه را لمس کن",540,2250,28,0xfff0e4cf,true,Layout.Alignment.ALIGN_CENTER,900);
            for(int i=1;i<=5;i++) drawFolder(c,i,folderX(i),folderY(i),i<=unlocked());
        }

        float folderX(int i){ return 155f + ((i-1)%3)*300f; }
        float folderY(int i){ return 520f + ((i-1)/3)*620f; }

        void drawFolder(Canvas c,int i,float x,float y,boolean open){
            int body=open?0xff7d5a36:0xff4c4b4a;
            round(c,x-92,y-75,x+92,y+85,18,body);
            round(c,x-72,y-93,x+5,y-55,12,body);
            text(c,"پرونده "+toFa(i),x,y-8,25,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,165);
            text(c,"●",x,y+40,22,open?0xffe3bd72:0xff8a8a8a,true,Layout.Alignment.ALIGN_CENTER,60);
        }

        void drawCaseRoom(Canvas c){
            drawPannedImage(c,caseRoomAmir,roomPan,120,2280);
            // Darken the bottom only to keep controls readable without hiding the scene.
            p.setColor(0x55000000); c.drawRect(0,2080,1080,2400,p);
            round(c,380,2150,700,2290,28,0xaa111b21);
            text(c,"برای دیدن اطراف، صفحه را چپ و راست بکش",540,2240,25,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,300);
            text(c,"امیر را لمس کن",540,2350,28,0xfff0dfbf,true,Layout.Alignment.ALIGN_CENTER,500);
        }

        void drawPannedImage(Canvas c, Bitmap b, float pan, float top, float bottom){
            if(b==null) return;
            float h=bottom-top, w=h*b.getWidth()/b.getHeight();
            float max=Math.max(0,w-1080f);
            float clamped=Math.max(0,Math.min(pan,max));
            c.drawBitmap(b,null,new RectF(-clamped,top,w-clamped,bottom),p);
        }

        void drawTopImagePreserveRatio(Canvas c, Bitmap b, float top, float targetWidth){
            if(b==null) return;
            float h=targetWidth*b.getHeight()/b.getWidth();
            c.drawBitmap(b,null,new RectF(0,top,targetWidth,top+h),p);
        }

        void drawCase1Amir(Canvas c){
            drawTopImagePreserveRatio(c,amirCloseup,120f,1080f);
            p.setColor(0x30000000); c.drawRect(0,120,1080,735,p);
            drawThoughtBubble(c,case1Dialogue==0 ? "ممنون که برای پرونده‌ام وقت گذاشتی." : "همه‌چیز از یک ماجرای مالی شروع شد.",760,260,360,240);
            panel(c,55,790,1025,2360,0xe9162027);
            if(case1Dialogue==0){
                text(c,"امیر",965,1635,30,0xffe2bb78,true,Layout.Alignment.ALIGN_OPPOSITE,850);
                text(c,"سلام. من امیرم. قبل از اینکه سراغ مدارک بریم، می‌خوام ماجرا رو برات توضیح بدم.",950,1740,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,850);
                round(c,110,1990,970,2180,28,0xff17435d);
                text(c,"چه اتفاقی افتاده؟",540,2110,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
            } else if(case1Dialogue==1){
                text(c,"امیر",965,1635,30,0xffe2bb78,true,Layout.Alignment.ALIGN_OPPOSITE,850);
                text(c,"یک اختلاف مالی پیش اومده و می‌خوام بدونم از نظر حقوقی چه مدارکی به دردم می‌خوره.",950,1740,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,850);
                round(c,110,1990,970,2180,28,0xff17435d);
                text(c,"چه مدرکی داری؟",540,2110,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
            } else {
                drawTopImagePreserveRatio(c,amirEvidenceDialogue,120f,1080f);
                panel(c,55,790,1025,2360,0xe9162027);
                text(c,"امیر",965,1635,30,0xffe2bb78,true,Layout.Alignment.ALIGN_OPPOSITE,850);
                text(c,"هرچی مدرک دارم داخل این کیفه.",950,1740,36,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,850);
                round(c,110,1990,970,2180,28,0xff6d4f2d);
                text(c,"بررسی کیف",540,2110,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
                text(c,"حالا کیف قابل بررسی است.",540,2270,27,0xffe5d5bc,true,Layout.Alignment.ALIGN_CENTER,700);
            }
        }

        void drawThoughtBubble(Canvas c,String s,float cx,float cy,float w,float h){
            p.setColor(0xeefffdf8); c.drawOval(new RectF(cx-w/2,cy-h/2,cx+w/2,cy+h/2),p);
            p.setColor(0xeefffdf8); c.drawCircle(cx-w*0.30f,cy+h*0.43f,24,p); c.drawCircle(cx-w*0.39f,cy+h*0.56f,13,p);
            text(c,s,cx,cy-48,27,0xff222222,true,Layout.Alignment.ALIGN_CENTER,w-45);
        }

        void drawCase1Bag(Canvas c){
            drawTopImagePreserveRatio(c,bagCloseup,120f,1080f);
            p.setColor(0x30000000); c.drawRect(0,120,1080,735,p);
            panel(c,55,790,1025,2320,0xe9162027);
            text(c,"رمز موبایل روی کیف دیده می‌شود",965,1660,31,0xffe2bb78,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,"چهار رقم را به خاطر بسپار؛ در مرحله بعد برای باز کردن گوشی به آن نیاز داری.",950,1780,30,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,850);
            round(c,130,1990,950,2170,28,0xff17435d);
            text(c,"باز کردن کیف",540,2105,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
        }

        void drawCase1Evidence(Canvas c){
            drawTopImagePreserveRatio(c,evidenceOpen,120f,1080f);
            p.setColor(0x26000000); c.drawRect(0,120,1080,728,p);
            panel(c,45,790,1035,2340,0xea101a21);
            text(c,"مدارک و اشیای قابل بررسی",970,1620,34,0xffe2bb78,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,"گوشی، رسیدهای پاره‌شده و دیسک فیلم مدار بسته را یکی‌یکی بررسی کن.",950,1745,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,880);
            text(c,"هر مورد یک سرنخ از پرونده امیر است.",950,1860,29,0xffd7cbb9,true,Layout.Alignment.ALIGN_OPPOSITE,880);
            if(phoneClueOpen && receiptsSolved && dvdSolved){
                round(c,100,1980,980,2190,28,0xff6d4f2d);
                text(c,"سه سرنخ کامل شد؛ وارد باکس تفکر شو",540,2110,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            } else {
                round(c,130,2010,950,2180,28,0xff17435d);
                text(c,"روی یکی از مدارک داخل کیف بزن",540,2120,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,820);
            }
        }

        void drawCase1Phone(Canvas c){
            p.setColor(0xff071015); c.drawRect(0,0,1080,2400,p);
            if(!phoneUnlocked){
                panel(c,70,270,1010,2240,0xff111c22);
                text(c,"گوشی امیر — قفل است",540,420,43,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
                text(c,"رمزی که روی کیف دیدی را وارد کن.",540,515,30,0xffd7cbb9,true,Layout.Alignment.ALIGN_CENTER,820);
                round(c,260,610,820,770,28,0xff172f3d);
                text(c,phoneEntry==0?"— — — —":toFa(phoneEntry),540,710,42,0xffe2bb78,true,Layout.Alignment.ALIGN_CENTER,520);
                String[] nums={"۱","۲","۳","۴","۵","۶","۷","۸","۹","پاک","۰","تأیید"};
                for(int i=0;i<12;i++){
                    int col=i%3,row=i/3; float l=180+col*240,t=900+row*230;
                    round(c,l,t,l+200,t+160,28,i==11?0xff6d4f2d:0xff173c51);
                    text(c,nums[i],l+100,t+102,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,180);
                }
                text(c,"رمز صحیح چهار رقمی است.",540,1915,27,0xffbda98a,true,Layout.Alignment.ALIGN_CENTER,760);
            } else {
                panel(c,60,250,1020,2230,0xff0f1a20);
                text(c,"گوشی امیر",950,390,42,0xffe2bb78,true,Layout.Alignment.ALIGN_OPPOSITE,850);
                text(c,"رمز درست بود. سه سرنخ را بررسی کن.",950,485,29,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,850);
                round(c,100,620,980,900,30,0xff173c51);
                text(c,"پیام‌ها",900,735,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,760);
                text(c,"۲ پیام خوانده‌نشده",900,815,27,0xffd7cbb9,true,Layout.Alignment.ALIGN_OPPOSITE,760);
                round(c,100,950,980,1230,30,0xff173c51);
                text(c,"سوابق انتقال",900,1065,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,760);
                text(c,"یک تراکنش با مبلغ ۱۵۰٬۰۰۰٬۰۰۰ ریال",900,1145,27,0xffd7cbb9,true,Layout.Alignment.ALIGN_OPPOSITE,760);
                round(c,100,1280,980,1560,30,0xff173c51);
                text(c,"عکس آخرین دوربین",900,1395,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,760);
                text(c,"زمان ثبت: ۲۱:۱۷",900,1475,27,0xffd7cbb9,true,Layout.Alignment.ALIGN_OPPOSITE,760);
                if(phoneClueOpen){
                    drawThoughtBubble(c,"پیام ساعت ۲۱:۱۴: «رسید رو گرفتم؛ بعد از خاموشی دوربین برمی‌گردم.»",540,1820,900,390);
                } else {
                    round(c,170,1780,910,1960,28,0xff6d4f2d);
                    text(c,"باز کردن پیام مشکوک",540,1890,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);
                }
            }
        }

        void drawCase1EvidenceDetail(Canvas c, int kind){
            if(kind==0){drawCase1Phone(c);return;}
            Bitmap b=evidenceOpen;
            if(b==null)return;
            Rect src; String title; String desc;
            if(kind==1){
                src=new Rect(470,390,920,760); title="رسیدهای پاره‌شده";
                desc="قطعات رسید را با کشیدن و رها کردن در جای درست قرار بده. ترتیب درست، سرنخ مالی پرونده را آشکار می‌کند.";
            } else {
                drawCase1Dvd(c); return;
            }
            p.setColor(0xff0a1116); c.drawRect(0,0,1080,2400,p);
            p.setColor(0xff101a21); c.drawRect(0,120,1080,1200,p);
            c.drawBitmap(b,src,new RectF(70,180,1010,1120),p);
            panel(c,55,1260,1025,2290,0xea101a21);
            text(c,title,965,1430,38,0xffe2bb78,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,desc,950,1555,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,850);
            drawReceiptPuzzle(c);
        }

        void drawReceiptPuzzle(Canvas c){
            String[] labels={"بانک مهر | ۲۱:۰۹ | ۵۰٬۰۰۰٬۰۰۰ ریال","انتقال وجه | ۲۱:۱۲ | ۱۰۰٬۰۰۰٬۰۰۰ ریال","جمع پرداخت | ۲۱:۱۳ | ۱۵۰٬۰۰۰٬۰۰۰ ریال"};
            for(int i=0;i<3;i++){
                float x=120+i*300, y=1760;
                if(receiptSlots[i]==-1){
                    round(c,x,y,x+250,y+180,22,0xff243039);
                    text(c,"جای قطعه "+toFa(i+1),x+125,y+105,25,0xffbcae9c,true,Layout.Alignment.ALIGN_CENTER,220);
                }
            }
            for(int i=0;i<3;i++) if(receiptSlots[i]==-1){
                float x=120+i*300, y=2050;
                round(c,x,y,x+250,y+180,18,0xffc9b79d);
                text(c,labels[i],x+125,y+58,21,0xff1d2529,true,Layout.Alignment.ALIGN_CENTER,220);
                text(c,"قطعه "+toFa(i+1),x+125,y+135,22,0xff4d4135,true,Layout.Alignment.ALIGN_CENTER,220);
            }
            if(receiptsSolved){
                drawThoughtBubble(c,"جمع رسیدها: ۱۵۰ میلیون ریال؛ همان مبلغی که در گوشی دیده شد.",540,2160,900,300);
            } else {
                text(c,"قطعات را از پایین بکش و در سه جای خالی رها کن.",540,2265,25,0xffd7cbb9,true,Layout.Alignment.ALIGN_CENTER,900);
            }
        }

        void drawCase1Dvd(Canvas c){
            p.setColor(0xff080f14); c.drawRect(0,0,1080,2400,p);
            panel(c,50,220,1030,2260,0xff101a21);
            text(c,"فیلم دوربین مداربسته",950,360,42,0xffe2bb78,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,"نوار زمان را حرکت بده و لحظه‌ای را پیدا کن که سرنخ پرونده دیده می‌شود.",950,455,29,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,860);
            round(c,90,560,990,1370,28,0xff1c292f);
            float frameX=150+(dvdTime/30f)*780f;
            text(c,"CAM 03",150,650,26,0xffb8c6cc,true,Layout.Alignment.ALIGN_LEFT,200);
            text(c,"21:"+String.format(java.util.Locale.US,"%02d",(int)dvdTime),930,650,28,0xffe2bb78,true,Layout.Alignment.ALIGN_RIGHT,200);
            if(dvdTime>=17 && dvdTime<=19){
                text(c,"خودروی تیره‌رنگ در ورودی پشتی",540,900,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
                text(c,"شخصی بسته‌ای را از صندوق بیرون می‌آورد.",540,1000,28,0xffd7cbb9,true,Layout.Alignment.ALIGN_CENTER,800);
                text(c,"سرنخ قابل ثبت: ۲۱:۱۷",540,1130,31,0xff9ee0ba,true,Layout.Alignment.ALIGN_CENTER,700);
            } else {
                text(c,dvdTime<17?"هنوز اتفاق اصلی دیده نمی‌شود":"لحظه موردنظر گذشته است",540,960,32,0xffd7cbb9,true,Layout.Alignment.ALIGN_CENTER,800);
            }
            round(c,130,1480,950,1660,30,0xff173c51);
            text(c,"نوار زمان را بکش",540,1590,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);
            round(c,120,1760,960,1815,20,0xff44535b);
            round(c,120,1745,120+840*(dvdTime/30f),1830,25,0xffc49a55);
            c.drawCircle(frameX,1787,24,p);
            if(dvdClueOpen) drawThoughtBubble(c,"زمان فیلم با زمان پیام گوشی و زمان رسیدها هم‌خوان است.",540,2030,900,330);
            else {round(c,180,1940,900,2120,28,0xff6d4f2d);text(c,"ثبت این لحظه به عنوان سرنخ",540,2050,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);}
        }

        void drawCase1Think(Canvas c){
            p.setColor(0xff071015); c.drawRect(0,0,1080,2400,p);
            text(c,"حالا نوبت فکر کردن است",540,390,48,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,900);
            text(c,"سه سرنخ را کنار هم بگذار و فرضیه مناسب را انتخاب کن.",540,500,29,0xffd7cbb9,true,Layout.Alignment.ALIGN_CENTER,850);
            drawThoughtBubble(c,"گوشی: پیام ساعت ۲۱:۱۴ و مبلغ ۱۵۰ میلیون",540,760,850,260);
            drawThoughtBubble(c,"رسیدها: جمع ۱۵۰ میلیون در ساعت ۲۱:۱۳",540,1110,850,260);
            drawThoughtBubble(c,"دوربین: سرنخ در ساعت ۲۱:۱۷",540,1460,850,260);
            String[] opts={"سه سرنخ از یک رخداد واحد پشتیبانی می‌کنند.","فقط رسید مهم است و دو سرنخ دیگر بی‌ارتباط‌اند.","فقط فیلم مهم است و اسناد مالی ارزشی ندارند."};
            for(int i=0;i<3;i++){
                float y=1660+i*210; int col=thoughtChoice==i?0xff6d4f2d:0xff173c51;
                round(c,90,y,990,y+160,25,col); text(c,opts[i],930,y+92,28,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,800);
            }
            if(thoughtChoice>=0){
                text(c,thoughtChoice==0?"استدلال منسجم است؛ حالا باید اقدام حقوقی بعدی را بررسی کنیم.":"این انتخاب با همه سرنخ‌های کشف‌شده سازگار نیست.",540,2330,27,thoughtChoice==0?0xff9ee0ba:0xffffb5ad,true,Layout.Alignment.ALIGN_CENTER,900);
                if(thoughtChoice==0){round(c,170,2160,910,2320,25,0xff17435d);text(c,"ثبت نتیجه تحقیق",540,2260,29,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);}
            }
        }

        void drawCase1Complete(Canvas c){
            p.setColor(0xff071015); c.drawRect(0,0,1080,2400,p);
            panel(c,70,430,1010,1980,0xff101a21);
            text(c,"مرحله تحقیقاتی کامل شد",540,650,48,0xffe2bb78,true,Layout.Alignment.ALIGN_CENTER,900);
            text(c,"گوشی، رسیدها و فیلم دوربین سه سرنخ مستقل بودند که با زمان و مبلغ مشترک به یکدیگر متصل شدند.",950,850,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,850);
            drawThoughtBubble(c,"پرونده هنوز تمام نشده؛ این فقط مرحله کشف و تطبیق سرنخ‌ها بود.",540,1250,850,300);
            round(c,140,1650,940,1830,30,0xff17435d);
            text(c,"بازگشت به اتاق پرونده",540,1760,33,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,760);
        }

        void drawCase(Canvas c) {
            int step=Math.min(storyStep,3);
            Bitmap person=caseCharacter(selectedCase);
            float targetH=step==0?760:690;
            drawCharacter(c,person,790,1450,targetH);
            panel(c,55,1540,1025,2240,0xe90c171e);
            text(c,"پرونده "+toFa(selectedCase)+"  |  "+casePeople[selectedCase],965,1630,28,0xffe0b96e,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,caseIssues[selectedCase],965,1715,30,0xffd7cbb9,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            text(c,story[selectedCase][step],965,1845,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            round(c,100,2090,980,2215,30,0xff17435d);
            text(c,step<3?"ادامه روایت":"ورود به بخش آموزشی",540,2170,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
        }

        Bitmap caseCharacter(int n){
            if(n==2 || n==5) return officer;
            if(n==4) return judge;
            if(n==3) return lawyer;
            return client;
        }

        void drawLesson(Canvas c) {
            panel(c,55,230,1025,2220,0xe9101920);
            text(c,"مفهوم حقوقی پرونده",970,390,40,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,880);
            text(c,lesson[selectedCase][0],970,510,34,0xfff2eadc,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            round(c,90,1330,990,1670,28,0xff1b3340);
            text(c,lesson[selectedCase][1],930,1435,29,0xffead9bc,true,Layout.Alignment.ALIGN_OPPOSITE,820);
            text(c,"حالا سؤال پرونده را حل کن.",540,1815,32,0xffe0c38f,true,Layout.Alignment.ALIGN_CENTER,800);
            round(c,120,1900,960,2070,30,0xff17435d);
            text(c,"ورود به تصمیم حقوقی",540,2005,36,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,800);
        }

        void drawQuiz(Canvas c) {
            panel(c,45,220,1035,2225,0xe9101920);
            text(c,questions[selectedCase],960,390,36,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,900);
            for(int i=0;i<3;i++){
                float top=650+i*360;
                int color=answerChosen ? (i==correctIndex[selectedCase]?0xff235c43:0xff422a2c) : 0xff173c51;
                round(c,95,top,985,top+245,28,color);
                text(c,options[selectedCase][i],930,top+105,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,800);
                if(answerChosen && i==correctIndex[selectedCase]) text(c,"✓",130,top+155,38,0xffb7e7c8,true,Layout.Alignment.ALIGN_CENTER,70);
            }
            if(answerChosen){
                text(c,correct?"پاسخ درست است":"پاسخ انتخابی درست نیست",540,1785,35,correct?0xff9ee0ba:0xffffb5ad,true,Layout.Alignment.ALIGN_CENTER,800);
                text(c,explanations[selectedCase],930,1880,29,0xffe7dac8,true,Layout.Alignment.ALIGN_OPPOSITE,820);
                round(c,170,2070,910,2185,28,0xff294552);
                text(c,"مشاهده نتیجه",540,2145,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);
            }
        }

        void drawResult(Canvas c) {
            panel(c,75,420,1005,1980,0xe90f1920);
            text(c,correct?"پرونده با موفقیت پیش رفت":"نیاز به مرور و تلاش دوباره",540,720,48,correct?0xffd8eddb:0xffffc1ba,true,Layout.Alignment.ALIGN_CENTER,850);
            text(c,correct?"مفهوم این پرونده به بخش پیشرفت اضافه شد.":"پاسخ را مرور کن و دوباره تصمیم بگیر.",540,835,31,0xffe5d9c8,true,Layout.Alignment.ALIGN_CENTER,850);
            round(c,150,1080,930,1250,30,0xff17435d);
            text(c,correct?(selectedCase<5?"بازگشت به کتابخانه":"پایان پنج پرونده") : "تلاش دوباره",540,1185,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,760);
        }

        void drawProgress(Canvas c) {
            panel(c,75,360,1005,2070,0xe90f1920);
            int completed=prefs.getInt("completed",0);
            text(c,"پیشرفت آموزشی",540,510,46,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            text(c,"پرونده‌های تکمیل‌شده: "+toFa(completed)+" از ۵",540,700,38,0xffeadfcf,true,Layout.Alignment.ALIGN_CENTER,850);
            for(int i=1;i<=5;i++){
                float y=850+(i-1)*210;
                round(c,150,y,930,y+150,24,i<=completed?0xff23543f:0xff263139);
                text(c,"پرونده "+toFa(i),850,y+62,30,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,650);
                text(c,i<=completed?"تکمیل شده":"در انتظار یادگیری",850,y+105,23,0xffd2c6b6,true,Layout.Alignment.ALIGN_OPPOSITE,650);
            }
        }

        void drawArchive(Canvas c) {
            panel(c,75,360,1005,2070,0xe90f1920);
            text(c,"بایگانی پرونده‌ها",540,510,44,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            int completed=prefs.getInt("completed",0);
            if(completed==0){text(c,"هنوز پرونده‌ای تکمیل نشده است.",540,800,32,0xffd8cdbd,true,Layout.Alignment.ALIGN_CENTER,850);return;}
            for(int i=1;i<=completed;i++){
                float y=700+(i-1)*245;
                text(c,"پرونده "+toFa(i),900,y,32,0xffe0bd7b,true,Layout.Alignment.ALIGN_OPPOSITE,700);
                text(c,titles[i],900,y+55,27,0xffeee5d8,true,Layout.Alignment.ALIGN_OPPOSITE,700);
            }
        }

        void drawLawBook(Canvas c) {
            panel(c,65,300,1015,2180,0xe90e1820);
            text(c,"کتاب قانون",540,440,48,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
            text(c,"راهنمای سریع مفاهیم این پنج پرونده",540,540,29,0xffd5bd98,true,Layout.Alignment.ALIGN_CENTER,850);
            String[] terms={"دادخواست","خواهان و خوانده","اجزای دادخواست","خواسته","دلیل"};
            String[] defs={"وسیله طرح دعوا از مسیر قانونی.","نقش طرفین دعوا را مشخص می‌کند.","اطلاعات ضروری برای شناسایی و بیان دعوا.","چیزی که خواهان از دادگاه درخواست می‌کند.","وسیله اثبات یا تقویت ادعا در حدود قانون."};
            for(int i=0;i<5;i++){
                float y=700+i*270;
                text(c,terms[i],930,y,32,0xffe1bb74,true,Layout.Alignment.ALIGN_OPPOSITE,760);
                text(c,defs[i],930,y+70,27,0xffeee6d9,true,Layout.Alignment.ALIGN_OPPOSITE,760);
            }
        }

        void drawSettings(Canvas c) {
            p.setColor(0x88000000); c.drawRect(0,0,1080,2400,p);
            panel(c,130,600,950,1800,0xf017252d);
            text(c,"تنظیمات",540,750,48,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);
            round(c,190,900,890,1080,30,0xff173c51);
            text(c,prefs.getBoolean("music",true)?"موسیقی: روشن":"موسیقی: خاموش",540,1010,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
            round(c,190,1160,890,1340,30,0xff294552);
            text(c,"افکت صدا: روشن",540,1270,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
            round(c,190,1420,890,1600,30,0xff3b2729);
            text(c,"خروج از بازی",540,1530,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,650);
        }

        void drawExit(Canvas c) {
            p.setColor(0xaa000000); c.drawRect(0,0,1080,2400,p);
            panel(c,140,900,940,1510,0xff17242c);
            text(c,"از بازی خارج می‌شوی؟",540,1040,42,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,750);
            round(c,200,1160,520,1310,28,0xff294552); text(c,"خیر",360,1255,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,260);
            round(c,560,1160,880,1310,28,0xff4a282b); text(c,"بله",720,1255,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,260);
        }

        void drawCharacter(Canvas c, Bitmap b, float cx, float bottom, float targetH) {
            if(b==null)return;
            float ratio=targetH/b.getHeight();
            float w=b.getWidth()*ratio;
            RectF d=new RectF(cx-w/2,bottom-targetH,cx+w/2,bottom);
            p.setAlpha(255); c.drawBitmap(b,null,d,p);
        }

        void round(Canvas c,float l,float t,float r,float b,float rad,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,rad,rad,p);}
        void panel(Canvas c,float l,float t,float r,float b,int color){round(c,l,t,r,b,34,color);}

        void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Layout.Alignment align,float width){
            tp.setTextSize(size); tp.setColor(color); tp.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));
            StaticLayout sl=StaticLayout.Builder.obtain(s,0,s.length(),tp,(int)width)
                    .setAlignment(align).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(true).build();
            float tx = align==Layout.Alignment.ALIGN_CENTER ? x-width/2f : x-width;
            c.save(); c.translate(tx,y); sl.draw(c); c.restore();
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            float x=(e.getX()-ox)/scale, y=(e.getY()-oy)/scale;
            if(screen==CASE_ROOM){
                return handleCaseRoomTouch(e,x,y);
            }
            if(screen==CASE1_PHONE || screen==CASE1_RECEIPTS || screen==CASE1_DVD){
                if(handleCase1PuzzleTouch(e,x,y)) return true;
            }
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;

            if(exitConfirm){
                if(in(x,y,560,1160,880,1310)){finishActivity();return true;}
                if(in(x,y,200,1160,520,1310)){exitConfirm=false;invalidate();return true;}
                return true;
            }
            if(settings){
                if(in(x,y,190,900,890,1080)){toggleMusic();invalidate();return true;}
                if(in(x,y,190,1160,890,1340)){Toast.makeText(getContext(),"افکت صدا در نسخه پایه فعال است.",Toast.LENGTH_SHORT).show();return true;}
                if(in(x,y,190,1420,890,1600)){askExit();return true;}
                if(in(x,y,60,0,1020,560)){settings=false;invalidate();return true;}
                return true;
            }

            if(screen==START){if(in(x,y,300,1030,780,1260)){goTo(OFFICE);}return true;}

            // Large, separated top controls: back on left, settings on right.
            if(screen!=OFFICE && in(x,y,25,20,170,145)){
                if(screen==CASE1_AMIR || screen==CASE1_BAG || screen==CASE1_EVIDENCE || screen==CASE1_PHONE || screen==CASE1_RECEIPTS || screen==CASE1_DVD || screen==CASE1_THINK || screen==CASE1_COMPLETE){
                    if(screen==CASE1_PHONE || screen==CASE1_RECEIPTS || screen==CASE1_DVD || screen==CASE1_THINK){goTo(CASE1_EVIDENCE);return true;}
                    if(screen==CASE1_COMPLETE){history.clear();history.push(LIBRARY);screen=CASE_ROOM;stopMusic();invalidate();return true;}
                    if(screen==CASE1_EVIDENCE){goTo(CASE1_BAG);return true;}
                    history.clear();history.push(LIBRARY);screen=CASE_ROOM;stopMusic();invalidate();return true;
                }
                goBack();return true;
            }
            if(in(x,y,900,20,1060,150)){settings=true;invalidate();return true;}

            if(screen==OFFICE){
                if(in(x,y,0,280,520,1370)){goTo(LIBRARY);return true;}
                if(in(x,y,650,820,930,1560)){goTo(ARCHIVE);return true;}
                if(in(x,y,850,1450,1080,2200)){goTo(PROGRESS);return true;}
                if(in(x,y,0,1760,310,2220)){goTo(LAWBOOK);return true;}
                if(in(x,y,300,1660,820,2300)){goTo(LIBRARY);return true;}
            }
            else if(screen==LIBRARY){
                for(int i=1;i<=5;i++){
                    float cx=folderX(i), cy=folderY(i);
                    if(in(x,y,cx-125,cy-125,cx+125,cy+125)){
                        if(i<=unlocked()){
                            selectedCase=i;
                            if(i==1){resetCase1New();goTo(CASE_ROOM);} else {storyStep=0;goTo(CASE);}
                        } else Toast.makeText(getContext(),"این پرونده هنوز قفل است.",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
            }
            else if(screen==CASE_ROOM){
                int action=e.getAction();
                if(action==MotionEvent.ACTION_DOWN){roomDownX=x;roomStartPan=roomPan;roomDragging=false;return true;}
                if(action==MotionEvent.ACTION_MOVE){
                    float dx=x-roomDownX;
                    if(Math.abs(dx)>12) roomDragging=true;
                    roomPan=Math.max(0,Math.min(roomMaxPan(),roomStartPan-dx));
                    invalidate(); return true;
                }
                if(action==MotionEvent.ACTION_UP){
                    float dx=x-roomDownX;
                    if(Math.abs(dx)>25 || roomDragging){roomDragging=false;return true;}
                    float h=2160f, imgW=h*caseRoomAmir.getWidth()/caseRoomAmir.getHeight();
                    float ix=(x+roomPan)/imgW*caseRoomAmir.getWidth();
                    float iy=(y-120)/h*caseRoomAmir.getHeight();
                    if(ix>=240 && ix<=780 && iy>=200 && iy<=700){case1Dialogue=0;goTo(CASE1_AMIR);return true;}
                    return true;
                }
                return true;
            }
            else if(screen==CASE1_AMIR){
                if(case1Dialogue==2 && (in(x,y,0,120,1080,735) || in(x,y,70,1950,1010,2220))){goTo(CASE1_BAG);return true;}
                if(in(x,y,100,1970,980,2200)){
                    if(case1Dialogue<2){case1Dialogue++; if(case1Dialogue==2) bagUnlocked=true; invalidate();}
                    return true;
                }
            }
            else if(screen==CASE1_BAG){
                if(in(x,y,80,120,1000,750)){goTo(CASE1_EVIDENCE);return true;}
                if(in(x,y,120,1980,960,2200)){history.clear();history.push(CASE_ROOM);screen=CASE_ROOM;stopMusic();invalidate();return true;}
            }
            else if(screen==CASE1_EVIDENCE){
                // Coordinates match the displayed 1536x865 asset scaled to 1080px wide.
                if(in(x,y,185,390,455,700)){goTo(CASE1_PHONE);return true;}
                if(in(x,y,320,385,655,735)){goTo(CASE1_RECEIPTS);return true;}
                if(in(x,y,595,385,860,720)){goTo(CASE1_DVD);return true;}
                if(phoneClueOpen && receiptsSolved && dvdSolved && in(x,y,100,1980,980,2220)){thoughtChoice=-1;goTo(CASE1_THINK);return true;}
            }
            else if(screen==CASE1_THINK){
                for(int i=0;i<3;i++) if(in(x,y,90,1660+i*210,990,1820+i*210)){thoughtChoice=i;invalidate();return true;}
                if(thoughtChoice==0 && in(x,y,120,2160,960,2330)){goTo(CASE1_COMPLETE);return true;}
            }
            else if(screen==CASE1_COMPLETE){
                if(in(x,y,120,1600,960,1880)){history.clear();history.push(CASE_ROOM);screen=CASE_ROOM;stopMusic();invalidate();return true;}
            }
            else if(screen==CASE){
                if(in(x,y,80,2040,1000,2250)){
                    if(storyStep<3){storyStep++;invalidate();} else goTo(LESSON);
                    return true;
                }
            }
            else if(screen==LESSON){if(in(x,y,100,1850,980,2100)){answerChosen=false;goTo(QUIZ);return true;}}
            else if(screen==QUIZ){
                if(answerChosen && in(x,y,120,2020,960,2210)){goTo(RESULT);return true;}
                if(!answerChosen){
                    for(int i=0;i<3;i++) if(in(x,y,80,630+i*360,1000,920+i*360)){
                        answerChosen=true; correct=i==correctIndex[selectedCase]; invalidate(); return true;
                    }
                }
            }
            else if(screen==RESULT){
                if(in(x,y,120,1050,960,1280)){
                    if(correct){
                        int done=prefs.getInt("completed",0);
                        if(selectedCase>done) prefs.edit().putInt("completed",selectedCase).putInt("unlocked",Math.min(5,selectedCase+1)).apply();
                        history.clear();
                        history.push(OFFICE);
                        screen=LIBRARY;
                        stopMusic();
                        invalidate();
                    } else {
                        answerChosen=false;
                        if(!history.isEmpty() && history.peek()==QUIZ) history.pop();
                        screen=QUIZ;
                        invalidate();
                    }
                    return true;
                }
            }
            return true;
        }

        boolean handleCase1PuzzleTouch(MotionEvent e,float x,float y){
            if(screen==CASE1_PHONE){
                if(e.getAction()!=MotionEvent.ACTION_UP) return true;
                if(!phoneUnlocked){
                    String[] nums={"۱","۲","۳","۴","۵","۶","۷","۸","۹","پاک","۰","تأیید"};
                    for(int i=0;i<12;i++){
                        int col=i%3,row=i/3; float l=180+col*240,t=900+row*230;
                        if(in(x,y,l,t,l+200,t+160)){
                            if(i==9) phoneEntry=0;
                            else if(i==11){ if(phoneEntry==1390){phoneUnlocked=true;phoneClueOpen=false;} else Toast.makeText(getContext(),"رمز صحیح نیست.",Toast.LENGTH_SHORT).show(); }
                            else {int d=(i==10?0:i+1); if(phoneEntry<1000) phoneEntry=phoneEntry*10+d;}
                            invalidate(); return true;
                        }
                    }
                } else if(in(x,y,100,620,980,900) || in(x,y,170,1780,910,1960)){
                    phoneClueOpen=true; invalidate(); return true;
                }
                if(in(x,y,120,1980,960,2200)){goTo(CASE1_EVIDENCE);return true;}
                return true;
            }
            if(screen==CASE1_RECEIPTS){
                if(e.getAction()==MotionEvent.ACTION_DOWN){
                    for(int i=0;i<3;i++){
                        if(receiptSlots[i]==-1){float x0=120+i*300,y0=2050;if(in(x,y,x0,y0,x0+250,y0+180)){draggedReceipt=i;dragX=x;dragY=y;return true;}}
                    }
                    return true;
                }
                if(e.getAction()==MotionEvent.ACTION_MOVE && draggedReceipt>=0){dragX=x;dragY=y;invalidate();return true;}
                if(e.getAction()==MotionEvent.ACTION_UP && draggedReceipt>=0){
                    int slot=-1; for(int i=0;i<3;i++){float x0=120+i*300,y0=1760;if(in(x,y,x0,y0,x0+250,y0+180)){slot=i;break;}}
                    if(slot>=0 && receiptSlots[slot]==-1) receiptSlots[slot]=draggedReceipt;
                    draggedReceipt=-1;
                    receiptsSolved=receiptSlots[0]==0&&receiptSlots[1]==1&&receiptSlots[2]==2;
                    invalidate(); return true;
                }
                return true;
            }
            if(screen==CASE1_DVD){
                if(e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE){
                    if(in(x,y,100,1680,980,1870)){dvdTime=Math.max(0,Math.min(30,(x-120)/840f*30f));invalidate();return true;}
                }
                if(e.getAction()==MotionEvent.ACTION_UP){
                    if(in(x,y,180,1940,900,2120) && dvdTime>=17 && dvdTime<=19){dvdSolved=true;dvdClueOpen=true;invalidate();return true;}
                    if(in(x,y,120,1980,960,2200)){goTo(CASE1_EVIDENCE);return true;}
                }
                return true;
            }
            return false;
        }

        boolean handleCaseRoomTouch(MotionEvent e,float x,float y){
            if(e.getAction()==MotionEvent.ACTION_DOWN){roomDownX=x;roomStartPan=roomPan;roomDragging=false;return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){
                float dx=x-roomDownX;
                if(Math.abs(dx)>12) roomDragging=true;
                roomPan=Math.max(0,Math.min(roomMaxPan(),roomStartPan-dx));
                invalidate(); return true;
            }
            if(e.getAction()==MotionEvent.ACTION_UP){
                float dx=x-roomDownX;
                if(Math.abs(dx)>25 || roomDragging){roomDragging=false;return true;}
                if(in(x,y,25,20,170,145)){goBack();return true;}
                if(in(x,y,900,20,1060,150)){settings=true;invalidate();return true;}
                float h=2160f;
                float imgW=h*caseRoomAmir.getWidth()/caseRoomAmir.getHeight();
                float ix=(x+roomPan)/imgW*caseRoomAmir.getWidth();
                float iy=(y-120)/h*caseRoomAmir.getHeight();
                if(ix>=240 && ix<=780 && iy>=190 && iy<=710){case1Dialogue=0;goTo(CASE1_AMIR);return true;}
                return true;
            }
            return true;
        }

        float roomMaxPan(){
            if(caseRoomAmir==null)return 0;
            float h=2160f;
            float w=h*caseRoomAmir.getWidth()/caseRoomAmir.getHeight();
            return Math.max(0,w-1080f);
        }

        boolean in(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;}
        int unlocked(){return Math.max(1,Math.min(5,prefs.getInt("unlocked",1)));}
        String toFa(int n){return String.valueOf(n).replace("0","۰").replace("1","۱").replace("2","۲").replace("3","۳").replace("4","۴").replace("5","۵").replace("6","۶").replace("7","۷").replace("8","۸").replace("9","۹");}

        void resetCase1New(){
            case1Dialogue=0;
            bagUnlocked=false;
            phoneUnlocked=false; phoneClueOpen=false; phoneEntry=0;
            receiptsSolved=false; dvdSolved=false; draggedReceipt=-1; receiptSlots=new int[]{-1,-1,-1};
            dvdTime=0f; dvdClueOpen=false; thoughtChoice=-1;
            roomPan=700f;
            roomDragging=false;
            roomDownX=0f;
            roomStartPan=700f;
        }

        void goTo(int next){
            if(screen!=next) history.push(screen);
            screen=next;
            if(next==LESSON||next==QUIZ||next==CASE||next==CASE_ROOM||next==CASE1_AMIR||next==CASE1_BAG||next==CASE1_EVIDENCE||next==CASE1_PHONE||next==CASE1_RECEIPTS||next==CASE1_DVD||next==CASE1_THINK||next==CASE1_COMPLETE) startMusic(); else stopMusic();
            invalidate();
        }

        boolean goBack(){
            if(exitConfirm||settings){exitConfirm=false;settings=false;invalidate();return true;}
            if(history.isEmpty())return false;
            screen=history.pop(); stopMusic(); invalidate(); return true;
        }

        void askExit(){exitConfirm=true;settings=false;invalidate();}
        void finishActivity(){((Activity)getContext()).finish();}

        void toggleMusic(){
            boolean on=prefs.getBoolean("music",true);
            prefs.edit().putBoolean("music",!on).apply();
            if(on) stopMusic(); else startMusic();
        }

        void startMusic(){
            if(!prefs.getBoolean("music",true))return;
            stopMusic();
            try{
                music=MediaPlayer.create(getContext(),R.raw.stage_music);
                if(music!=null){music.setLooping(true);music.setVolume(.55f,.55f);music.start();}
            }catch(Exception ignored){music=null;}
        }

        void stopMusic(){
            if(music!=null){try{music.stop();}catch(Exception ignored){} try{music.release();}catch(Exception ignored){} music=null;}
        }
    }
}
