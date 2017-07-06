package com.eshangke.framework.util;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by luoliwen on 16/5/31.
 */
public class RxSampleUtils {

    private static final String TAG = RxSampleUtils.class.getSimpleName();

    private OkHttpClient client;

    public RxSampleUtils() {
        client = new OkHttpClient();
    }

    /**
     * 声明一个被观察者对象，作为结果返回
     * @param path
     * @return
     */
    public Observable<byte[]> downLoadImage(String path){
        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override
            public void call(Subscriber<? super byte[]> subscriber) {
                if (!subscriber.isUnsubscribed()){
                    //访问网络操作
                    Request request = new Request.Builder().url(path).build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            subscriber.onError(e);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                byte[] data = response.body().bytes();
                                if (data != null) {
                                    subscriber.onNext(data);
                                }
                            }
                            subscriber.onCompleted();
                        }
                    });

                }
            }
        });
    }
    /**
     * 使用create方式
     */
    public static void createObserable() {
        //定义被观察者，
        Observable<String> observable = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext("hello");
                    subscriber.onNext("hi");
                    subscriber.onNext(downLoadJson());
                    subscriber.onNext("world");
                    subscriber.onCompleted();
                }
            }
        });

        Subscriber<String> showsub = new Subscriber<String>() {
            @Override
            public void onCompleted() {
                Log.i(TAG, "onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, e.getMessage());
            }

            @Override
            public void onNext(String s) {
                Log.i(TAG, "result-->>" + s);
            }
        };
        observable.subscribe(showsub);//关联被观察者

    }

    /**
     * 调用下载方法
     *
     * @return
     */
    public static String downLoadJson() {
        return "json data";
    }

    /**
     * create 第二种方式
     */
    public static void createPrint() {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    for (int i = 1; i <= 10; i++) {
                        subscriber.onNext(i);
                    }
                    subscriber.onCompleted();
                }
            }
        }).subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                Log.i(TAG, "onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, e.getMessage());
            }

            @Override
            public void onNext(Integer integer) {
                Log.i(TAG, "result-->>" + integer);
            }
        });
    }

    /***
     * 使用在被观察者，返回的对象一般都是数值类型
     */
    public static void from() {
        Integer[] items = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Observable observable = Observable.from(items);
        observable.subscribe(new Action1() {
            @Override
            public void call(Object o) {
                Log.i(TAG, o.toString());
            }
        });
    }

    /**
     * 指定某一时刻进行数据发送
     */
    public static void interval() {
        Integer[] items = {1, 2, 3, 4, 5};
        Observable observable = Observable.interval(1, 1, TimeUnit.SECONDS);//每一个发送数据
        observable.subscribe(new Action1() {
            @Override
            public void call(Object o) {
                Log.i(TAG, o.toString());
            }
        });
    }

    /**
     * 处理数组集合
     */
    public static void just() {
        Integer[] items1 = {1, 2, 3, 4, 5, 6};
        Integer[] items2 = {3, 5, 6, 8, 3, 8};
        Observable observable = Observable.just(items1, items2);
        observable.subscribe(new Subscriber<Integer[]>() {
            @Override
            public void onCompleted() {
                Log.i(TAG, "onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, e.getMessage());
            }

            @Override
            public void onNext(Integer[] o) {
                for (int i = 0; i < o.length; i++) {
                    Log.i(TAG, "next:" + o[i]);
                }
            }
        });
    }

    /**
     * 使用范围数据，指定输出数据的范围
     */
    public static void range() {
        Observable observable = Observable.range(1, 40);
        observable.subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                Log.i(TAG, "onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                Log.i(TAG, e.getMessage());
            }

            @Override
            public void onNext(Integer o) {
                Log.i(TAG, "next:" + o);
            }
        });
    }

    /**
     * 使用过滤功能
     */
    public static void filter() {
        Observable observable = Observable.just(1, 2, 3, 4, 5, 7, 8);
        observable.filter(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer o) {
                return o < 5;
            }
        }).observeOn(Schedulers.io()).subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer o) {
                Log.i(TAG, o.toString());
            }
        });
    }
}
