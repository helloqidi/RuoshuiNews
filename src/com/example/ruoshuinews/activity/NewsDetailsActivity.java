package com.example.ruoshuinews.activity;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.example.ruoshuinews.R;
import com.example.ruoshuinews.model.Parameter;
import com.example.ruoshuinews.service.SyncHttp;


public class NewsDetailsActivity extends Activity
{
	private ViewFlipper mNewsBodyFlipper;
	private LayoutInflater mNewsBodyInflater;
	private float mStartX;
	private int mCount;

	private ArrayList<HashMap<String, Object>> mNewsData;	//传递过来的新闻列表信息
	private int mPosition = 0;	//当前新闻在新闻列表中的位置
	private int mNid;
	
	private final int FINISH = 0;
	private TextView mNewsDetails;	//新闻详情所在的View
	private Button mNewsdetailsTitlebarComm;// 新闻回复数
	private int mCursor;
	
	private ImageButton mNewsReplyImgBtn;// 发表新闻回复图片
	private LinearLayout mNewsReplyImgLayout;// 发表新闻回复图片Layout
	private LinearLayout mNewsReplyEditLayout;// 发表新闻回复编辑框Layout
	private TextView mNewsReplyContent;// 新闻回复编辑框
	
	//new一个Handler
	private Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.arg1)
			{
			case FINISH:
				// 把获取到的新闻显示到界面上
				mNewsDetails.setText(Html.fromHtml(msg.obj.toString()));
				break;
			}
		}
	};
	
	//异步获取新闻详情的线程
	//在这个线程里面不能操作UI
	private class UpdateNewsThread extends Thread
	{
		@Override
		public void run()
		{
			// 从网络上获取新闻
			String newsBody = getNewsBody();
			Message msg = mHandler.obtainMessage();
			msg.arg1 = FINISH;
			msg.obj = newsBody;
			mHandler.sendMessage(msg);
		}
	}
	

	//发表回复的线程
	//在这个线程中，可以操作UI，因为调用方法是: mNewsReplyEditLayout.post(new PostCommentThread())，这个方法把线程放到主线程中执行。
	private class PostCommentThread extends Thread
	{
		@Override
		public void run()
		{
			SyncHttp syncHttp = new SyncHttp();
			String url = "http://192.168.3.80:9292/postComment";
			List<Parameter> params = new ArrayList<Parameter>();
			params.add(new Parameter("nid", mNid + ""));	//后面加+""是为了变为String类型
			params.add(new Parameter("region", "江苏省连云港市"));
			params.add(new Parameter("content", mNewsReplyContent.getText().toString()));
			try
			{
				String retStr = syncHttp.httpPost(url, params);
				JSONObject jsonObject = new JSONObject(retStr);
				int retCode = jsonObject.getInt("ret");
				if (0 == retCode)
				{
					Toast.makeText(NewsDetailsActivity.this, R.string.post_success, Toast.LENGTH_SHORT).show();
					mNewsReplyImgLayout.setVisibility(View.VISIBLE);
					mNewsReplyEditLayout.setVisibility(View.GONE);
					return;
				}

			} catch (Exception e)
			{
				e.printStackTrace();
			}
			Toast.makeText(NewsDetailsActivity.this, R.string.post_failure, Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newsdetails);
		
		// 查找新闻回复图片Layout
		mNewsReplyImgLayout = (LinearLayout) findViewById(R.id.news_reply_img_layout);
		// 查找新闻回复回复Layout
		mNewsReplyEditLayout = (LinearLayout) findViewById(R.id.news_reply_edit_layout);
		// 新闻回复内容
		mNewsReplyContent = (TextView) findViewById(R.id.news_reply_edittext);
		
		//给TitleBar中的Button设置OnClickListener
		Button newsDetailsTitlebarPref = (Button)findViewById(R.id.newsdetails_titlebar_previous);
		NewsDetailsOnClickListener newsDetailsOnClickListener = new NewsDetailsOnClickListener();
		newsDetailsTitlebarPref.setOnClickListener(newsDetailsOnClickListener);
		Button newsDetailsTitlebarNext = (Button)findViewById(R.id.newsdetails_titlebar_next);
		newsDetailsTitlebarNext.setOnClickListener(newsDetailsOnClickListener);
		//新闻回复数Button
		mNewsdetailsTitlebarComm = (Button)findViewById(R.id.newsdetails_titlebar_comments);
		mNewsdetailsTitlebarComm.setOnClickListener(newsDetailsOnClickListener);
		// 发表新闻回复图片Button
		mNewsReplyImgBtn = (ImageButton) findViewById(R.id.news_reply_img_btn);
		mNewsReplyImgBtn.setOnClickListener(newsDetailsOnClickListener);
		// 发表回复
		Button newsReplyPost = (Button) findViewById(R.id.news_reply_post);
		newsReplyPost.setOnClickListener(newsDetailsOnClickListener);
		
		
		//获取传递的信息
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		// 设置标题栏名称
		String categoryName = bundle.getString("categoryName");
		TextView titleBarTitle = (TextView) findViewById(R.id.newsdetails_titlebar_title);
		titleBarTitle.setText(categoryName);
		//获取新闻集合
		Serializable s  = bundle.getSerializable("newsDate");
		mNewsData = (ArrayList<HashMap<String, Object>>) s;
		//获取点击位置
		mCursor = mPosition = bundle.getInt("position");

		// 动态创建新闻视图，并赋值
		mNewsBodyInflater = getLayoutInflater();
		inflateView(0);
	}
	
	/**
	 * 处理NewsDetails点击事件
	 */
	class NewsDetailsOnClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			//键盘输入管理对象
			InputMethodManager m = (InputMethodManager) mNewsReplyContent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
			
			switch (v.getId())
			{
			// 上一条新闻
			case R.id.newsdetails_titlebar_previous:
				showPrevious();
				break;
			// 下一条新闻
			case R.id.newsdetails_titlebar_next:
				showNext();
				break;
			// 显示评论
			case R.id.newsdetails_titlebar_comments:
				Intent intent = new Intent(NewsDetailsActivity.this, CommentsActivity.class);
				startActivity(intent);
				break;
			// 新闻回复图片
			case R.id.news_reply_img_btn:
				mNewsReplyImgLayout.setVisibility(View.GONE);
				mNewsReplyEditLayout.setVisibility(View.VISIBLE);
				mNewsReplyContent.requestFocus();	//光标聚焦到输入框
				m.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT);	//显示键盘
				break;
			// 发表新闻回复
			case R.id.news_reply_post:
				mNewsReplyEditLayout.post(new PostCommentThread());
				mNewsReplyImgLayout.setVisibility(View.VISIBLE);
				mNewsReplyEditLayout.setVisibility(View.GONE);
				m.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
				break;
			}
		}
	}
	
	/**
	 * 处理新闻NewsBody滑动效果
	 */
	class NewsBodyOnTouchListener implements OnTouchListener
	{
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			switch (event.getAction())
			{
			//手指按下
			case MotionEvent.ACTION_DOWN:
				//记录起始坐标
				mStartX = event.getX();
				// 设置新闻回复Layout是否可见
				mNewsReplyImgLayout.setVisibility(View.VISIBLE);
				mNewsReplyEditLayout.setVisibility(View.GONE);
				// 隐藏键盘
				InputMethodManager m = (InputMethodManager) mNewsReplyContent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				m.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
				break;
			//手指抬起
			case MotionEvent.ACTION_UP:
				//往左滑动
				if (event.getX() < mStartX)
				{
					showPrevious();
				}
				//往右滑动
				else if (event.getX() > mStartX)
				{
					showNext();
				}
				break;
			}
			return true;
		}
	}
	
	/**
	 * 获取新闻详细信息
	 * @return
	 */
	private String getNewsBody()
	{
		String retStr = "网络连接失败，请稍后再试";
		SyncHttp syncHttp = new SyncHttp();
		String url = "http://192.168.3.80:9292/getNews";
		String params = "nid=" + mNid;
		try
		{
			String retString = syncHttp.httpGet(url, params);
			JSONObject jsonObject = new JSONObject(retString);
			//获取返回码，0表示成功
			int retCode = jsonObject.getInt("ret");
			if (0 == retCode)
			{
				JSONObject dataObject = jsonObject.getJSONObject("data");
				JSONObject newsObject = dataObject.getJSONObject("news");
				retStr = newsObject.getString("body");
			}

		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return retStr;
	}
	
	/**
	 * 显示下一条新闻
	 */
	private void showNext()
	{
		//判断是否是最后一篇win问
		if (mPosition<mNewsData.size()-1)
		{
			// 设置下一屏动画
			mNewsBodyFlipper.setInAnimation(this, R.anim.push_left_in);
			mNewsBodyFlipper.setOutAnimation(this, R.anim.push_left_out);
			mPosition++;
			//记录当前新闻编号
			HashMap<String, Object> hashMap = mNewsData.get(mPosition);
			mNid = (Integer) hashMap.get("nid");
			//判断下一屏是否已经创建
			if (mPosition >= mNewsBodyFlipper.getChildCount())
			{
				inflateView(mNewsBodyFlipper.getChildCount());
			}
			// 显示下一屏
			mNewsBodyFlipper.showNext();
		}
		else
		{
			Toast.makeText(this, R.string.no_next_news, Toast.LENGTH_SHORT).show();
		}
		System.out.println(mCursor +";"+mPosition);
	}

	private void showPrevious()
	{
		if (mPosition>0)
		{
			mPosition--;
			//记录当前新闻编号
			HashMap<String, Object> hashMap = mNewsData.get(mPosition);
			mNid = (Integer) hashMap.get("nid");
			if (mCursor>mPosition)
			{
				mCursor = mPosition;
				inflateView(0);
				System.out.println(mNewsBodyFlipper.getChildCount());
				//mNewsBodyFlipper.showNext();// 显示下一页
			}
			mNewsBodyFlipper.setInAnimation(this, R.anim.push_right_in);// 定义下一页进来时的动画
			mNewsBodyFlipper.setOutAnimation(this, R.anim.push_right_out);// 定义当前页出去的动画
			mNewsBodyFlipper.showPrevious();// 显示上一页
		}
		else
		{
			Toast.makeText(this, R.string.no_pre_news, Toast.LENGTH_SHORT).show();
		}
		System.out.println(mCursor +";"+mPosition);
	}
	
	//获取新闻信息(包括异步获取新闻详情并更新UI)
	private void inflateView(int index)
	{
		// 动态创建新闻视图，并赋值
		View newsBodyLayout = mNewsBodyInflater.inflate(R.layout.news_body, null);
		// 获取点击新闻基本信息
		HashMap<String, Object> hashMap = mNewsData.get(mPosition);
		// 新闻标题
		TextView newsTitle = (TextView) newsBodyLayout.findViewById(R.id.news_body_title);
		newsTitle.setText(hashMap.get("newslist_item_title").toString());
		// 发布时间和出处
		TextView newsPtimeAndSource = (TextView) newsBodyLayout.findViewById(R.id.news_body_ptime_source);
		newsPtimeAndSource.setText(hashMap.get("newslist_item_ptime").toString() + "    " + hashMap.get("newslist_item_source").toString());
		// 新闻编号
		mNid = (Integer) hashMap.get("nid");
		// 新闻回复数
		mNewsdetailsTitlebarComm.setText(hashMap.get("newslist_item_comments") + "跟帖");
	
		// 把新闻视图添加到Flipper中
		mNewsBodyFlipper = (ViewFlipper) findViewById(R.id.news_body_flipper);
		mNewsBodyFlipper.addView(newsBodyLayout,index);
	
		// 给新闻Body添加触摸事件
		mNewsDetails = (TextView) newsBodyLayout.findViewById(R.id.news_body_details);
		mNewsDetails.setOnTouchListener(new NewsBodyOnTouchListener());
	
		// 启动线程
		new UpdateNewsThread().start();
	}	
	
//	public final String NEWS = "<p>　　环比全部停涨首次出现</p>\r\n<p>　　数据显示，在新建商品住宅方面，2012年1月全国70个大中城市，价格环比下降的城市有48个，持平的城市有22个，没有一个城市出现上涨。从房价价格指数公布来看，首次出现了新建商品住宅环比全部停涨的现象。</p>\r\n<p>　　从同比看，70个大中城市中，价格下降的城市有15个，比去年12月份增加6个。1月份，同比涨幅回落的城市有50个，涨幅均未超过3.9%。</p>\r\n<p>　　二手住宅</p>\r\n<p>　　仅有5个城市环比上涨</p>\r\n<p>　　从二手房看,与上月相比，70个大中城市中，价格下降的城市有54个，持平的城市有11个。与去年12月份相比，1月份环比价格下降的城市增加了3个。环比上涨的仅5个城市：分别为贵阳、济宁、襄阳、韶关、遵义，均仅上涨0.1%。同比看，70个大中城市中，价格下降的城市有37个，比去年12月份增加了8个。1月份，同比涨幅回落的城市有29个，涨幅均未超过3.5%。</p>\r\n<p>　　北京情况</p>\r\n<p>　　二手房环比同比均下跌</p>\r\n<p>　　数据显示，北京二手房价格不管是环比还是同比，都在下跌，且下跌幅度均有所加大。</p>\r\n<p>　　在环比方面，自去年8月份出现停涨后，二手房价格环比开始下跌，且此后每个月的下跌幅度在不断加大，到2012年1月份，其环比下跌幅度已达到0.9%。而在同比方面，2012年1月份下跌3.1%，创下最大跌幅。</p>\r\n<p>　　北京新建住宅价格指数2010年5月时同比涨幅为22%，而到了2012年1月同比涨幅仅为0.1%。新建商品房价格指数同比涨幅也是连续下跌，下降幅度也比较大，去年年底同比涨幅为1.3%，而到了今年1月份，则同比涨幅下跌为0.1%。在环比方面，继2011年10月首次出现下跌后，环比继续下跌为0.1%。</p>\r\n<p>　　■ 分析</p>\r\n<p>　　北京房价调控成效明显</p>\r\n<p>　　北京中原地产市场总监张大伟认为，北京作为限购执行最严格的城市，房价调控已经见到明显成效。限购导致的直接需求减少，限购抑制投资、投机，在北京出台的最严格版限购下，5年外地户籍限购年限使得购房者回归自住，本地需求占据9成，自住首套占据9成，全市最近一年成交量中投资及投机基本绝迹。</p>\r\n<p>　　同时，买卖双方博弈加剧，限购限贷使得购房者期待价格下调，但是投资手段匮乏、通货膨胀依然是阻碍价格下滑的关键因素。特别是城区部分二手房房主依然坚挺价格，惜售，这使得在价格依然居高不下的情况下，购房者入市谨慎。</p>\r\n<p>　　■ 预测</p>\r\n<p>　　今年房价或下调10%-20%</p>\r\n<p>　　张大伟认为，2012年限购政策不会放松，一线城市楼市拐点已经明确，预期在6-12个月内房价可能还有10%-20%的下调，而且一线城市对全国的示范作用非常大，不仅在政策执行力度上，在房价下调过程中也会明显影响全国。</p>\r\n<p>　　链家地产市场研究部冯联联认为，在楼市调控效果继续巩固的背景下，降价趋势仍会持续。1月份全国多个城市新房市场成交跌入谷底，观望情绪依旧浓重，为加速销售回款，将不断有开发商加入降价换量阵营，新房价格预期会进一步下调。</p>";	
}
