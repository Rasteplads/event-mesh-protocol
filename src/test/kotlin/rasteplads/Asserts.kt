package rasteplads

import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <reified T: Throwable> assertJobCancels(job: Job){
    job.join()
    // TODO: Use T
    if (!job.isCancelled)
        throw AssertionError("Expected job to be cancelled, but it was not.")
}