package dev.parfenov.sowa.schema.plugin.config.gradle;

/**
 * Extension для конфигурации Gradle plugin генерации Sowa схем.
 * Содержит все параметры конфигурации, аналогичные Maven plugin.
 *
 * @author <a href="https://github.com/kirillparfenov">Kirill Parfenov</a>
 * @since 2025-08-10
 */
public class SowaSchemaExtension {

    /**
     * Имя профиля Sowa для конфигурации инфраструктуры
     */
    private String sowaProfileName = "SOWA_PROFILE_NAME";

    /**
     * Ветка для сравнения изменений при использовании git diff режима
     */
    private String branchDiffWith = "origin/develop";

    /**
     * Флаг для обработки только измененных в git файлов
     */
    private boolean onlyGitDiff = false;

    /**
     * Флаг для извлечения определений в отдельные файлы
     */
    private boolean extractDefinitions = false;

    /**
     * Процент увеличения длины строк для валидации
     */
    private int stringLengthIncreasePercent = 0;

    /**
     * Базовые пакеты проекта для сканирования классов
     */
    private String[] projectPackages = new String[0];

    // Геттеры и сеттеры

    public String getSowaProfileName() {
        return sowaProfileName;
    }

    public void setSowaProfileName(String sowaProfileName) {
        this.sowaProfileName = sowaProfileName;
    }

    public String getBranchDiffWith() {
        return branchDiffWith;
    }

    public void setBranchDiffWith(String branchDiffWith) {
        this.branchDiffWith = branchDiffWith;
    }

    public boolean isOnlyGitDiff() {
        return onlyGitDiff;
    }

    public void setOnlyGitDiff(boolean onlyGitDiff) {
        this.onlyGitDiff = onlyGitDiff;
    }

    public boolean isExtractDefinitions() {
        return extractDefinitions;
    }

    public void setExtractDefinitions(boolean extractDefinitions) {
        this.extractDefinitions = extractDefinitions;
    }

    public int getStringLengthIncreasePercent() {
        return stringLengthIncreasePercent;
    }

    public void setStringLengthIncreasePercent(int stringLengthIncreasePercent) {
        this.stringLengthIncreasePercent = stringLengthIncreasePercent;
    }

    public String[] getProjectPackages() {
        return projectPackages;
    }

    public void setProjectPackages(String[] projectPackages) {
        this.projectPackages = projectPackages;
    }
}