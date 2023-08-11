# KotlinRetrier

🪨 Rock-solid, concise and thorough library to retry and repeat `suspend` tasks.

## A cold retrier with all options

```kotlin
var conditionFlow: Flow<Bool>

// Fully configurable policy with good defaults. 
// Also available: withConstantDelay(), withNoDelay()
val coldRetrier = withExponentialBackoff() 
    // Fetch only when you've got network 
    // and your user is authenticated for example
    .onlyWhen(condition)
    // Ensure your retrier fails on some conditions
    .giveUpAfterMaxAttempts(10)
    .giveUpOnErrors {
        it is MyFatalError
    }
    // Ensure your retrier won't give up on some errors, even if `maxAttempts`
    // has been reached or the error is a `MyFatalError`
    .retryOnErrors {
        it is MyTmpError
    }
```

**All giveUp / retry modifiers are evaluated in reversed order.**

[Exponential backoff](https://aws.amazon.com/fr/blogs/architecture/exponential-backoff-and-jitter/) with
full jitter is the default and recommended algorithm to fetch from a backend.

## Task retrier

Call `.task { doSomething() }` to obtain a new retrier ready to be executed, in the form of a 
`Flow<RetrierEvent<Output>>`, `Output` being the return type of your task.

At this point it's not started yet. It will start as soon as you either collect its flow or await for a final value.

**WARNING** Collecting the same retrier flow multiple times will start multiple task execution sequences in parallel.

However, you can reuse any cold retrier to execute multiple tasks independently:

```kotlin
val fetcher = coldRetrier
    .task { 
        fetchSomething() 
    }

val poller = coldRetrier
    // If you want to poll, well you can
    .repeatingWithDelay(30.seconds)
    .task { 
        fetchSomethingElse() 
    }

val otherFetcher = coldRetrier { fetchSomethingElse() }

```

## Await value in concurrency context

If you don't repeat, you can directly wait for the single task value in a coroutine context

```kotlin
// This will throw the last attempt error if any `giveUp*()` function matches
val value = withExponentialBackoff() 
    .onlyWhen(conditionFlow)
    .giveUpAfterMaxAttempts(10)
    .giveUpOnErrors {
        it is MyFatalError
    }
    .retryOnErrors {
        it is MyTmpError
    }
    .task {
        api.fetchValue()
    }
    .value()
```

## Flows

All retriers (including repeaters) are flows that emit relevant events.

```kotlin
poller
    .collect {
        when (it) {
            is AttemptSuccess ->  { 
                println("Fetched something: ${it.value}")
            }
            is AttemptFailure -> {
                println("An attempt #${it.index} failed with ${it.error})")
            }
            is Completion -> {
                println("Poller completed with ${it.error?.toString() ?: "no error"}")
            }
        }
    }
```

The collection finishes after any completion or immediately if a completion was emitted before, but does not throw.

If you're only interested in actual task values, you can use `values()` flow operator.

If you want to observe errors but collect only values for example, you can use [`onEach()`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/on-each.html)
flow operator.

```kotlin
poller
  .onEach {
      if (it is AttemptFailure) {
          println("Got error: ${it.error}")
      }
  }
  .values()
  .collect {
      println("Got value: $it")
  }
```

### Actual retrier classes

Finally, you can use the classes initializers directly, namely `SimpleRetrier`,
`ConditionalRetrier` and `Repeater`.

## Technical aspects

### Conflation proof

Retriers are cold flows, implying that when they emit, they'll wait for the collector to execute before resuming.

This way, you can't miss any event.

If you use any conflating operator for example by sharing a retrier flow using `shareIn()`, the resulting flow will
be subject to [conflation](https://kotlinlang.org/docs/flow.html#conflation), and you may miss events.

### Cancellation

To cancel a retrier, just cancel its collection:

```kotlin
val job = launch {
    poller
        .collect {
            // Handle events
        }
}

fun cancelPoller() {
    job.cancel()
}
```

It's guaranteed that retriers won't emit anything after being cancelled.

## Retriers contract

- Retriers retry until either:
    - their policy gives up
    - the task succeeds (except for repeaters that will delay another trial)
    - the retrier's job is cancelled
    - their conditionFlow completes after having published no value or `false` as its last value
- When a policy gives up, the last task error is thrown on any `value()`, and is embedded into
  a `Completion`.
- All retriers start retrying their task on first flow subscription. Subsequent subscriptions to the flow **will** start
different retry sequences.
- After a retrier is interrupted then resumed by its `conditionFlow`, its policy is reused from start with the first
failure index to `0`. However, attempt indexes output by its flow take previous failures into account and are strictly
increasing. 
- In case of a repeater, the output failure index is reset to `0` after each success.
- All retriers honor flows [exception transparency](https://kotlinlang.org/docs/flow.html#exception-transparency).

## Retry Policies

It's important to understand that policies are not used to repeat after a success, but only to retry on failure.
When repeating, the policy is reused from start after each success.

### Built-in retry policies

**ExponentialBackoffRetryPolicy** is implemented according to state-of-the-art algorithms.
Have a look to the available arguments, and you'll recognize the standard parameters and options.
You can especially choose the jitter type between `None`, `Full` (default) and `Decorrelated`.

**ConstantDelayRetryPolicy** policy does what you expect, just waiting for a fixed amount of time.

You can add failure conditions using `giveUp*()` functions, and bypass these conditions using `retry*()` functions.

All giveUp / retry modifiers are evaluated in reversed order.

### Homemade policy

You can create your own policies that conform `RetryPolicy` and they will benefit from the same modifiers.
Have a look at `ConstantDelayRetryPolicy.kt` for a basic example.

To create a DSL entry point using your policy:

```kotlin
fun withMyOwnPolicy(): ColdRetrier {
    val policy = MyOwnPolicy()
    return ColdRetrier(policy, null)
}
```

## Contribute

Feel free to make any comment, criticism, bug report or feature request using GitHub issues.
You can also directly email me at `pierre` *strange "a" with a long round tail* `pittscraft.com`.

## License

KotlinRetrier is available under the MIT license. See the LICENSE file for more info.
