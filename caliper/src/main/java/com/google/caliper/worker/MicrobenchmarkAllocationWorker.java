/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.caliper.worker;

import com.google.caliper.model.Measurement;
import com.google.caliper.runner.Running.Benchmark;
import com.google.caliper.runner.Running.BenchmarkMethod;
import com.google.inject.Inject;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * The {@link Worker} for the {@code AllocationInstrument}.  This class invokes the benchmark method
 * a few times, with varying numbers of reps, and computes the number of object allocations and the
 * total size of those allocations.
 */
public final class MicrobenchmarkAllocationWorker extends Worker {
  private static final int MAX_BASELINE_REPS = 5;
  private static final int MAX_REPS_ABOVE_BASELINE = 100;

  private final Random random;
  private final AllocationRecorder recorder;

  @Inject MicrobenchmarkAllocationWorker(@Benchmark Object benchmark, 
      @BenchmarkMethod Method method, AllocationRecorder recorder, Random random) {
    super(benchmark, method);
    this.random = random;
    this.recorder = recorder;
  }
  
  @Override public void bootstrap() throws Exception {
    // do one initial measurement and throw away its results
    measureAllocations(benchmark, benchmarkMethod, 1);
  }

  @Override public Iterable<Measurement> measure() throws Exception {
    // [1, 5]
    int baselineReps = random.nextInt(MAX_BASELINE_REPS) + 1;
    AllocationStats baseline = measureAllocations(benchmark, benchmarkMethod, baselineReps);
    // (baseline, baseline + MAX_REPS_ABOVE_BASELINE]
    int measurementReps = baselineReps + random.nextInt(MAX_REPS_ABOVE_BASELINE) + 1;
    AllocationStats measurement = measureAllocations(benchmark, benchmarkMethod, measurementReps);
    return measurement.minus(baseline).toMeasurements();
  }

  private AllocationStats measureAllocations(
      Object benchmark, Method method, int reps) throws Exception {
    // do the Integer boxing and the creation of the Object[] outside of the record block, so that
    // our internal allocations aren't counted in the benchmark's allocations.
    Object[] args = {reps};
    recorder.startRecording();
    method.invoke(benchmark, args);
    return recorder.stopRecording(reps);
  }
}
