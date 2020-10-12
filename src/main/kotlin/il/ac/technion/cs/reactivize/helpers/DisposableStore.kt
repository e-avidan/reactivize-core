package il.ac.technion.cs.reactivize.helpers

import io.reactivex.rxjava3.disposables.Disposable

class DisposableStore {
    companion object {
        @JvmStatic // Make life easier for me
        fun create() = DisposableStore()
    }

    var disposable: Disposable? = null
        set(disposable) {
            field?.dispose()
            field = disposable
        }
}