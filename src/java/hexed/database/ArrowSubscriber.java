package hexed.database;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import arc.util.Nullable;

public class ArrowSubscriber<T> implements Subscriber<T> {
    private Callback<Subscription> subscribe;
    private Callback<T> next;
    private Callback<?> complete;
    private Callback<Throwable> error;
    private boolean isReturnedValue = false;

    public ArrowSubscriber() {
        this(subscribe -> subscribe.request(1), null, null, null);
    }
    
    public ArrowSubscriber(
        Callback<Subscription> subscribe,
        Callback<T> next,
        Callback<Nullable> complete,
        Callback<Throwable> error
    ) {
        this.complete = complete;
        this.error = error;
        this.next = next;
        this.subscribe = subscribe;
    }

    @Override
    public void onSubscribe(Subscription s) {
        try {
            if (this.subscribe == null) return;
            this.subscribe.call(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNext(T t) {
        try {
            isReturnedValue = false;
            if (this.next == null) return;
            this.next.call(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onComplete() {
        try {
            if (!isReturnedValue) this.onNext(null);
            if (this.complete == null) return;
            this.complete.call(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Throwable t) {
        try {
            if (this.error == null) throw t;
            this.error.call(t);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
