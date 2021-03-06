/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.ql.exec.vector.expressions;

import org.apache.hadoop.hive.ql.exec.vector.LongColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorExpressionDescriptor;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;

/**
 * Evaluates the boolean complement of the input.
 */
public class NotCol extends VectorExpression {
  private static final long serialVersionUID = 1L;

  private final int colNum;

  public NotCol(int colNum, int outputColumnNum) {
    super(outputColumnNum);
    this.colNum = colNum;
  }

  public NotCol() {
    super();

    // Dummy final assignments.
    colNum = -1;
  }

  @Override
  public void evaluate(VectorizedRowBatch batch) {

    if (childExpressions != null) {
      super.evaluateChildren(batch);
    }

    LongColumnVector inputColVector = (LongColumnVector) batch.cols[colNum];
    int[] sel = batch.selected;
    int n = batch.size;
    long[] vector = inputColVector.vector;
    LongColumnVector outV = (LongColumnVector) batch.cols[outputColumnNum];
    long[] outputVector = outV.vector;

    if (n <= 0) {
      // Nothing to do, this is EOF
      return;
    }

    if (inputColVector.noNulls) {
      outV.noNulls = true;
      if (inputColVector.isRepeating) {
        outV.isRepeating = true;
        // 0 XOR 1 yields 1, 1 XOR 1 yields 0
        outputVector[0] = vector[0] ^ 1;
      } else if (batch.selectedInUse) {
        for (int j = 0; j != n; j++) {
          int i = sel[j];
          outputVector[i] = vector[i] ^ 1;
        }
        outV.isRepeating = false;
      } else {
        for (int i = 0; i != n; i++) {
          outputVector[i] = vector[i] ^ 1;
        }
        outV.isRepeating = false;
      }
    } else {
      outV.noNulls = false;
      if (inputColVector.isRepeating) {
        outV.isRepeating = true;
        outputVector[0] = vector[0] ^ 1;
        outV.isNull[0] = inputColVector.isNull[0];
      } else if (batch.selectedInUse) {
        outV.isRepeating = false;
        for (int j = 0; j != n; j++) {
          int i = sel[j];
          outputVector[i] = vector[i] ^ 1;
          outV.isNull[i] = inputColVector.isNull[i];
        }
      } else {
        outV.isRepeating = false;
        for (int i = 0; i != n; i++) {
          outputVector[i] = vector[i] ^ 1;
          outV.isNull[i] = inputColVector.isNull[i];
        }
      }
    }
  }

  @Override
  public String vectorExpressionParameters() {
    return getColumnParamString(0, colNum);
  }

  @Override
  public VectorExpressionDescriptor.Descriptor getDescriptor() {
    return (new VectorExpressionDescriptor.Builder())
        .setMode(
            VectorExpressionDescriptor.Mode.PROJECTION)
        .setNumArguments(1)
        .setArgumentTypes(
            VectorExpressionDescriptor.ArgumentType.INT_FAMILY)
        .setInputExpressionTypes(
            VectorExpressionDescriptor.InputExpressionType.COLUMN).build();
  }
}
