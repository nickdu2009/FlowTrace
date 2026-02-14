package com.flowtrace.infra.sunnynet

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared holder for the current Android TUN file descriptor.
 *
 * Why here:
 * - Domain layer must not depend on Android/VpnService.
 * - VpnCaptureService lives in app module, CaptureEngine lives in infra module.
 *
 * Contract:
 * - VpnCaptureService should set() after establish().
 * - CaptureEngine should consumeOnce() when starting SunnyNet Tun driver.
 */
@Singleton
class TunFdProvider @Inject constructor() {
  private val fd = AtomicInteger(UNSET)

  fun set(newFd: Int) {
    fd.set(newFd)
  }

  fun clear() {
    fd.set(UNSET)
  }

  fun peek(): Int? = fd.get().takeIf { it >= 0 }

  fun consumeOnce(): Int? {
    val v = fd.getAndSet(UNSET)
    return v.takeIf { it >= 0 }
  }

  private companion object {
    private const val UNSET = -1
  }
}

