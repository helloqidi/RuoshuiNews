package com.example.ruoshuinews.view;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Html;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.ruoshuinews.R;


/**
 *@author coolszy
 *@date 2012-5-1
 *@blog http://blog.92coding.com
 */

public class CustomTextView extends LinearLayout
{
	private Context mContext;
	private TypedArray mTypedArray;
	
	public CustomTextView(Context context)
	{
		this(context, null);
	}

	public CustomTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
		
		setOrientation(LinearLayout.VERTICAL);
		mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.customTextView);

	}

	public void setText(ArrayList<HashMap<String, Object>> datas)
	{
		for (HashMap<String, Object> hashMap : datas)
		{
			//如果是图片
			if (hashMap.get("type").equals("image"))
			{
				int imageWidth = mTypedArray.getDimensionPixelOffset(R.styleable.customTextView_image_width, 100);
				int imageheight = mTypedArray.getDimensionPixelOffset(R.styleable.customTextView_image_height, 100);
				//创建ImageView并设置属性
				ImageView imageView = new ImageView(mContext);
				LayoutParams params = new LayoutParams(imageWidth, imageheight);
				params.gravity = Gravity.CENTER_HORIZONTAL;//居中
				imageView.setLayoutParams(params);
				imageView.setImageResource(R.drawable.kuka);//默认图片
				imageView.setScaleType(ScaleType.CENTER_INSIDE);
				addView(imageView);
				//启动线程，异步加载图片
				new DownloadPicThread(imageView,hashMap.get("value").toString()).start();
			} 
			//反之则已文本显示
			else
			{
				//创建TextView并设置属性
				TextView textView = new TextView(mContext);
				textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				float textSize = mTypedArray.getDimension(R.styleable.customTextView_textSize, 16);
				int textColor = mTypedArray.getColor(R.styleable.customTextView_textColor, 0xFF000000);
				textView.setTextSize(textSize);
				textView.setTextColor(textColor);
				textView.setText(Html.fromHtml(hashMap.get("value").toString()));
				addView(textView);
			}
		}
	}

	private Handler mHandler = new Handler()
	{
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg)
		{
			HashMap<String, Object> hashMap = (HashMap<String, Object>)msg.obj;
			ImageView imageView = (ImageView)hashMap.get("imageview");
			LayoutParams params = new LayoutParams(msg.arg1,msg.arg2);
			params.gravity = Gravity.CENTER_HORIZONTAL;//居中
			imageView.setLayoutParams(params);
			Drawable drawable = (Drawable)hashMap.get("drawable");
			imageView.setImageDrawable(drawable);
		}
		
	};
	
	private class DownloadPicThread extends Thread
	{
		private ImageView mImageView;
		private String mUrl;

		public DownloadPicThread(ImageView imageView, String url)
		{
			super();
			this.mImageView = imageView;
			this.mUrl = url;
		}

		public void run()
		{
			Drawable drawable = null;
			int newImgWidth = 0;
			int newImgHeigth = 0;
			try
			{
				drawable = Drawable.createFromStream(new URL(mUrl).openStream(), "image");
				newImgWidth = drawable.getIntrinsicWidth() / 3;
				newImgHeigth = drawable.getIntrinsicHeight() / 3;
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			//为了更好的看到效果，让线程休眠2秒
			SystemClock.sleep(2000);
			//使用Handler更新UI
			Message msg = mHandler.obtainMessage();
			HashMap<String, Object> hashMap = new HashMap<String, Object>();
			hashMap.put("imageview", mImageView);
			hashMap.put("drawable", drawable);
			msg.obj = hashMap;
			msg.arg1 = newImgWidth;
			msg.arg2 = newImgHeigth;
			mHandler.sendMessage(msg);
		}
	}
}