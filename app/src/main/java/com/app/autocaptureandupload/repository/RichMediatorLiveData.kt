package com.app.autocaptureandupload.repository

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.app.autocaptureandupload.models.FailureResponseKotlin


abstract class  RichMediatorLiveData <T> : MediatorLiveData<T>() {
    private lateinit var errorLiveData: MutableLiveData<Throwable?>
    private lateinit var failureResponseLiveData: MutableLiveData<FailureResponseKotlin?>

    open fun initLiveData() {
        errorLiveData = MutableLiveData()
        failureResponseLiveData = MutableLiveData()
    }

    protected abstract fun getFailureObserver(): Observer<FailureResponseKotlin?>

    protected abstract fun getErrorObserver(): Observer<Throwable?>

     override fun onInactive() {
        super.onInactive()
        removeSource(failureResponseLiveData)
        removeSource(errorLiveData)
    }

    override fun onActive() {
        initLiveData()
        super.onActive()



        addSource(failureResponseLiveData, getFailureObserver())
        addSource(errorLiveData, getErrorObserver())
    }

    open fun setFailure(failureResponse: FailureResponseKotlin) {
        failureResponseLiveData.postValue(failureResponse)
    }

    open fun setError(t: Throwable) {
        errorLiveData.postValue(t)
    }
}