package pl.guz.rxplugin;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class SchedulerHandlerTest {

    private static StringBuilder caller;

    @Before
    @After
    public void setup() {
        RxJavaPlugins.reset();
        caller = new StringBuilder();
    }

    @Test
    public void should_scheduler_handler_calls_twice_when_is_subscribe_on_new_thread_test() {
        // given:
        AtomicInteger counter = new AtomicInteger(1);
        RxJavaPlugins.setScheduleHandler(scheduleHandler(counter));

        // when:
        Observable.fromCallable(this::futureRunnable)
                  .subscribeOn(Schedulers.newThread())
                  .blockingSubscribe(Runnable::run);

        // then:
        Assert.assertEquals("scheduleHandler-[1]scheduleHandler-[2]hello", caller.toString());
    }

    @Test
    public void should_scheduler_handler_calls_any_one_when_is_subscribe_on_the_same_thread_test() {
        // given:
        AtomicInteger counter = new AtomicInteger(1);
        RxJavaPlugins.setScheduleHandler(scheduleHandler(counter));

        // when:
        Observable.fromCallable(this::futureRunnable)
                  .blockingSubscribe(Runnable::run);

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
        RxJavaPlugins.setScheduleHandler(scheduleHandler(counter));

        // when:
        Observable.fromCallable(this::futureRunnable)
                  .observeOn(scheduler)
                  .observeOn(scheduler2)
                  .blockingSubscribe(Runnable::run);

        // then:
        Assert.assertEquals("scheduleHandler-[1]scheduleHandler-[2]hello", caller.toString());
    }

    @Test
    public void should_scheduler_handler_calls_four_times_when_thread_pool_is_changed_twice_and_is_subscribe_on_new_thread() throws InterruptedException {
        // given:
        Scheduler scheduler = Schedulers.from(Executors
                                                      .newSingleThreadExecutor(new CustomizableThreadFactory("test-pool-1-")));
        Scheduler scheduler2 = Schedulers.from(Executors
                                                       .newSingleThreadExecutor(new CustomizableThreadFactory("test-pool-2-")));
        AtomicInteger counter = new AtomicInteger(1);
        RxJavaPlugins.setScheduleHandler(scheduleHandler(counter));

        // when:
        Observable.fromCallable(this::futureRunnable)
                  .observeOn(scheduler)
                  .doOnNext(runnable -> log.info("First observeOn"))
                  .observeOn(scheduler2)
                  .doOnNext(runnable -> log.info("Second observeOn"))
                  .subscribeOn(Schedulers.newThread())
                  .doOnSubscribe(disposable -> log.info("Subscribe"))
                  .blockingSubscribe(Runnable::run);

        // then:
        Assert.assertEquals("scheduleHandler-[1]scheduleHandler-[2]scheduleHandler-[3]scheduleHandler-[4]hello", caller.toString());
    }

    private Function<Runnable, Runnable> scheduleHandler(AtomicInteger counter) {
        return runnable -> {
            log.info("Call scheduleHandler {}", counter.get());
            caller.append("scheduleHandler-[")
                  .append(counter.getAndIncrement())
                  .append("]");
            return runnable;
        };
    }

    private Runnable futureRunnable() {
        return () -> {
            log.info("call runnable");
            caller.append("hello");
        };
    }
}
