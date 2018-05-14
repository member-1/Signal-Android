package org.thoughtcrime.securesms;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SignalExecutors {

  public static final Executor DATABASE = Executors.newSingleThreadExecutor();
  public static final Executor DISK_IO  = Executors.newSingleThreadExecutor();
}
