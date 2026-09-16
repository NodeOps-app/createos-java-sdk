package sh.createos.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Sandbox CPU, memory, and disk sizing preset. */
public record Shape(
    String id,
    @JsonProperty("vcpu") int virtualCpu,
    @JsonProperty("mem_mib") int memoryMib,
    @JsonProperty("default_disk_mib") long defaultDiskMib,
    @JsonProperty("cpu_quota_pct") int cpuQuotaPercent) {}
