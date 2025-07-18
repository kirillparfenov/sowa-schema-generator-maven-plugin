/**
 * @author Kirill Parfenov
 * @see https://github.com/kirillparfenov
 * @since 2025
 */
package dev.parfenov.sowa.schema.plugin.generator;

import static dev.parfenov.sowa.schema.plugin.generator.ValidationConstants.*;

/**
 * Калькулятор длины строк с учетом процента увеличения.
 * <p>
 * Предоставляет методы для расчета увеличенной длины строк
 * с округлением до ближайшего подходящего значения.
 */
public final class StringLengthCalculator {
    
    private final int stringLengthIncreasePercent;
    
    /**
     * Создает калькулятор с указанным процентом увеличения.
     * 
     * @param stringLengthIncreasePercent процент увеличения длины (0-100)
     */
    public StringLengthCalculator(int stringLengthIncreasePercent) {
        this.stringLengthIncreasePercent = stringLengthIncreasePercent;
    }
    
    /**
     * Увеличивает длину строки на заданный процент с округлением.
     * 
     * @param originalLength исходная длина строки
     * @return увеличенная и округленная длина
     */
    public Integer increaseLength(Integer originalLength) {
        if (originalLength == null || stringLengthIncreasePercent <= 0) {
            return originalLength;
        }
        
        var increasedValue = originalLength * (1 + stringLengthIncreasePercent / 100.0);
        int step = increasedValue < LENGTH_BOUNDARY ? SMALL_LENGTH_STEP : LARGE_LENGTH_STEP;
        
        return (int) (Math.round(increasedValue / step) * step);
    }
} 