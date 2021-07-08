package net.opendasharchive.openarchive.util.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun <R> CoroutineScope.executeAsyncTask(
    onPreExecute: () -> Unit,
    doInBackground: () -> R,
    onPostExecute: (R) -> Unit
) = launch {
    onPreExecute()
    val result = withContext(Dispatchers.Default) {
        doInBackground()
    }
    onPostExecute(result)
}

fun <R> CoroutineScope.executeAsyncTaskWithList(
    onPreExecute: () -> Unit,
    doInBackground: () -> List<R>,
    onPostExecute: (List<R>) -> Unit
) = launch {
    onPreExecute()
    val result = withContext(Dispatchers.Default) {
        doInBackground()
    }
    onPostExecute(result)
}

fun CoroutineScope.runOnBackground(
    block: suspend () -> Unit
) = launch {
    withContext(Dispatchers.Default) {
        block()
    }
}