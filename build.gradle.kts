plugins {
    id("base")
    id("idea")
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
