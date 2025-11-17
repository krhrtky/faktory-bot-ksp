package com.example.faktory.ksp

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Factory(val tableName: String)
