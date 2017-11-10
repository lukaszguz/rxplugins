package pl.guz.rxplugin;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SchedulerHookTest {

    private static StringBuilder caller;

    @Before
    @After
    public void setup() {
        RxJavaHooks.reset();
        caller = new StringBuilder();
    }

    @Test
    public void should_scheduler_handler_calls_twice_when_is_subscribe_on_new_thread_test() {
        // given:
        AtomicInteger counter = new AtomicInteger(1);
        RxJavaHooks.setOnScheduleAction(scheduleHandler(counter));

        // when:
        Observable.fromCallable(this::fixtureAction)
                  .subscribeOn(Schedulers.newThread())
                  .toBlocking()
                  .subscribe(Action0::call);

        // then:
        Assert.assertEquals("scheduleHandler-[1]hello", caller.toString());
    }

    @Test
    public void should_scheduler_handler_calls_any_one_when_is_subscribe_on_the_same_thread_test() {
        // given:
        AtomicInteger counter = new AtomicInteger(1);
        RxJavaHooks.setOnScheduleAction(scheduleHandler(counter));

        // when:
        Observable.fromCallable(this::fixtureAction)
                  .toBlocking()
                  .subscribe(Action0::call);

        // then:
        Assert.assertEquals("hello", caller.toString());
    }

    @Test
    public void should_scheduler_handler_calls_twice_when_thread_pool_is_changed_twice() {
        // given:
        Scheduler scheduler = Schedulers.from(Executors
                                                      .newSingleThreadExecutor(new CustomizableThreadFactory("test-pool-1-")));
        Scheduler scheduler2 = Schedulers.from(Executors
                                                       .newSingleThreadExecutor(new CustomizableThreadFactory("test-pool-2-")));
        AtomicInteger counter = new AtomicInteger(1);
        RxJavaHooks.setOnScheduleAction(scheduleHandler(counter));

        // when:
        Observable.fromCallable(this::fixtureAction)
                  .observeOn(scheduler)
                  .observeOn(scheduler2)
                  .toBlocking()
                  .subscribe(Action0::call);

        // then:
        Assert.assertEquals("scheduleHandler-[1]scheduleHandler-[2]scheduleHandler-[3]hello", caller.toString());
    }

    @Test
    public void should_scheduler_handler_calls_four_times_when_thread_pool_is_changed_twice_and_is_subscribe_on_new_thread() throws InterruptedException {
        // given:
        Scheduler scheduler = Schedulers.from(Executors
                                                      .newSingleThreadExecutor(new CustomizableThreadFactory("test-pool-1-")));
        Scheduler scheduler2 = Schedulers.from(Executors
                                                       .newSingleThreadExecutor(new CustomizableThreadFactory("test-pool-2-")));
        AtomicInteger counter = new AtomicInteger(1);
        RxJavaHooks.setOnScheduleAction(scheduleHandler(counter));

        // when:
        Observable.fromCallable(this::fixtureAction)
                  .observeOn(scheduler)
                  .doOnNext(runnable -> log.info("First observeOn"))
                  .observeOn(scheduler2)
                  .doOnNext(runnable -> log.info("Second observeOn"))
                  .subscribeOn(Schedulers.newThread())
                  .doOnSubscribe(() -> log.info("Subscribe"))
                  .toBlocking()
                  .subscribe(Action0::call);

        // then:
        Assert.assertEquals("scheduleHandler-[1]scheduleHandler-[2]scheduleHandler-[3]scheduleHandler-[4]hello", caller.toString());
    }

    private Func1<Action0, Action0> scheduleHandler(AtomicInteger counter) {
        return action -> {
            log.info("Call scheduleHandler {}", counter.get());
            caller.append("scheduleHandler-[")
                  .append(counter.getAndIncrement())
                  .append("]");
            return action;
        };
    }

    private Action0 fixtureAction() {
        return () -> {
            log.info("call runnable");
            caller.append("hello");
        };
    }
}
