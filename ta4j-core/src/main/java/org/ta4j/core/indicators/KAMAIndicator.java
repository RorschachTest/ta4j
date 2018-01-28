/*
  The MIT License (MIT)

  Copyright (c) 2014-2017 Marc de Verdelhan, Ta4j Organization & respective authors (see AUTHORS)

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal in
  the Software without restriction, including without limitation the rights to
  use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
  the Software, and to permit persons to whom the Software is furnished to do so,
  subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ta4j.core.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.Num.Num;

/**
 * The Kaufman's Adaptive Moving Average (KAMA)  Indicator.
 * 
 * @see <a href="http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:kaufman_s_adaptive_moving_average">
 *     http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:kaufman_s_adaptive_moving_average</a>
 */
public class KAMAIndicator extends RecursiveCachedIndicator<Num> {

    private final Indicator<Num> price;
    
    private final int timeFrameEffectiveRatio;
    
    private final Num fastest;
    
    private final Num slowest;
    
    /**
     * Constructor.
     *
     * @param price the price
     * @param timeFrameEffectiveRatio the time frame of the effective ratio (usually 10)
     * @param timeFrameFast the time frame fast (usually 2)
     * @param timeFrameSlow the time frame slow (usually 30)
     */
    public KAMAIndicator(Indicator<Num> price, int timeFrameEffectiveRatio, int timeFrameFast, int timeFrameSlow) {
        super(price);
        this.price = price;
        this.timeFrameEffectiveRatio = timeFrameEffectiveRatio;
        fastest = numOf(2).dividedBy(numOf(timeFrameFast + 1));
        slowest = numOf(2).dividedBy(numOf(timeFrameSlow + 1));
    }

    @Override
    protected Num calculate(int index) {
        Num currentPrice = price.getValue(index);
        if (index < timeFrameEffectiveRatio) {
            return currentPrice;
        }
        /*
         * Efficiency Ratio (ER)
         * ER = Change/Volatility
         * Change = ABS(Close - Close (10 periods ago))
         * Volatility = Sum10(ABS(Close - Prior Close))
         * Volatility is the sum of the absolute value of the last ten price changes (Close - Prior Close).
         */
        int startChangeIndex = Math.max(0, index - timeFrameEffectiveRatio);
        Num change = currentPrice.minus(price.getValue(startChangeIndex)).abs();
        Num volatility = numOf(0);
        for (int i = startChangeIndex; i < index; i++) {
            volatility = volatility.plus(price.getValue(i + 1).minus(price.getValue(i)).abs());
        }
        Num er = change.dividedBy(volatility);
        /*
         * Smoothing Constant (SC)
         * SC = [ER x (fastest SC - slowest SC) + slowest SC]2
         * SC = [ER x (2/(2+1) - 2/(30+1)) + 2/(30+1)]2
         */
        Num sc = er.multipliedBy(fastest.minus(slowest)).plus(slowest).pow(2);
        /*
         * KAMA
         * Current KAMA = Prior KAMA + SC x (Price - Prior KAMA)
         */
        Num priorKAMA = getValue(index - 1);
        return priorKAMA.plus(sc.multipliedBy(currentPrice.minus(priorKAMA)));
    }

}
