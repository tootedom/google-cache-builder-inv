package org.greencheek.spark;

/**
 * Adapted from https://github.com/holman/spark
 *
 * whose license is:
 *
 *

 The MIT License

 Copyright (c) Zach Holman, http://zachholman.com

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 associated documentation files (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial
 portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 */

/**
 * User: dominictootell
 * Date: 02/03/2013
 * Time: 17:25
 */
public class Spark {

    public static char[] TICKS = new char[]{'▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'};
    public static int NUMBER_OF_TICKS = TICKS.length-1;

    public static void main(String[] args) throws Exception {
        for(char s : TICKS) {
            System.out.println(s);

        }

        System.out.println(new String(new byte[]{-30,-106,-118}) + new String(new byte[]{-30,-106,-125}) + new String(new byte[]{-30,-106,-119})+ new String(new byte[]{-30,-106,-119}));
        System.out.println(graph(new int[]{10,3,4,5,6,90,389,28,3}));
    }

    public static SparkResult graph(int[] numbers) {

        char[] graph = new char[numbers.length];

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for(int num : numbers) {
            if(num<min) min = num;
            if(num>max) max = num;
        }

        int f=(( ((max-min)<<8)/(NUMBER_OF_TICKS) ));
        if(f<1) f = 1;

        int numberOfNumbers = numbers.length;
        for(int i =0;i< numberOfNumbers;i++) {
             graph[i] = TICKS[ (((numbers[i]-min)<<8)/f) ];
        }

        return new SparkResult(graph,min,max);
    }


    public static class SparkResult {
        private final int min;
        private final int max;
        private char[] graph;

        public SparkResult(char[] results,int min, int max) {
            this.min = min;
            this.max = max;
            this.graph = results;
        }

        public char[] getGraph() {
            return graph;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }


        public String graphToString() {
            return new String(graph);
        }

        public String toString() {
            StringBuilder str = new StringBuilder(new String(graph));
            str.append(':').append("min/max:").append(min).append('/').append(max);
            return str.toString();
        }

    }


}
