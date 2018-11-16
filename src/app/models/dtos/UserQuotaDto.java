package models.dtos;

public class UserQuotaDto {
    private Long totalQuota;

    private Long totalUsedQuota;
    private Long totalFreeQuota;

    private Long totalUsedTitleQuota;
    private Long totalUsedCommentQuota;
    private Long totalUsedFileDataQuota;
    private Long totalTempFilesQuota;

    private Double usedQuotaPercent;
    private Double freeQuotaPercent;

    private Double usedTitleQuotaPercent;
    private Double usedCommentQuotaPercent;
    private Double usedFileDataQuotaPercent;
    private Double usedTempFilesQuotaPercent;

    public UserQuotaDto(Long totalQuota, Long totalUsedTitleQuota, Long totalUsedCommentQuota, Long totalUsedFileDataQuota, Long totalTempFilesQuota) {
        this.totalQuota = totalQuota;
        this.totalUsedTitleQuota = totalUsedTitleQuota;
        this.totalUsedCommentQuota = totalUsedCommentQuota;
        this.totalUsedFileDataQuota = totalUsedFileDataQuota;
        this.totalTempFilesQuota = totalTempFilesQuota;

        this.totalUsedQuota = this.totalUsedTitleQuota + this.totalUsedCommentQuota + this.totalUsedFileDataQuota + this.totalTempFilesQuota;
        this.totalFreeQuota = this.totalQuota - this.totalUsedQuota;

        this.usedQuotaPercent = this.totalUsedQuota.doubleValue() / this.totalQuota.doubleValue();
        this.freeQuotaPercent = this.totalFreeQuota.doubleValue() / this.totalQuota.doubleValue();

        this.usedTitleQuotaPercent = this.totalUsedTitleQuota.doubleValue() / this.totalQuota.doubleValue();
        this.usedCommentQuotaPercent = this.totalUsedCommentQuota.doubleValue() / this.totalQuota.doubleValue();
        this.usedFileDataQuotaPercent = this.totalUsedFileDataQuota.doubleValue() / this.totalQuota.doubleValue();
        this.usedTempFilesQuotaPercent = this.totalTempFilesQuota.doubleValue() / this.totalQuota.doubleValue();
    }

    public Long getTotalQuota() {
        return totalQuota;
    }

    public Long getTotalUsedQuota() {
        return totalUsedQuota;
    }

    public Long getTotalFreeQuota() {
        return totalFreeQuota;
    }

    public Long getTotalUsedTitleQuota() {
        return totalUsedTitleQuota;
    }

    public Long getTotalUsedCommentQuota() {
        return totalUsedCommentQuota;
    }

    public Long getTotalUsedFileDataQuota() {
        return totalUsedFileDataQuota;
    }

    public Long getTotalTempFilesQuota() {
        return totalTempFilesQuota;
    }

    public Double getUsedQuotaPercent() {
        return usedQuotaPercent;
    }

    public Double getFreeQuotaPercent() {
        return freeQuotaPercent;
    }

    public Double getUsedTitleQuotaPercent() {
        return usedTitleQuotaPercent;
    }

    public Double getUsedCommentQuotaPercent() {
        return usedCommentQuotaPercent;
    }

    public Double getUsedFileDataQuotaPercent() {
        return usedFileDataQuotaPercent;
    }

    public Double getUsedTempFilesQuotaPercent() {
        return usedTempFilesQuotaPercent;
    }
}
