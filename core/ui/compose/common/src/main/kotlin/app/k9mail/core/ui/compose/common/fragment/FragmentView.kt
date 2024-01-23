package app.k9mail.core.ui.compose.common.fragment

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.fragment.app.findFragment
import app.k9mail.core.ui.compose.common.activity.LocalActivity

// Based on androidx.compose.ui.viewinterop.AndroidViewBinding() and
// https://stackoverflow.com/questions/60520145/fragment-container-in-jetpack-compose/70817794#70817794
@Composable
fun FragmentView(
    fragmentFactory: () -> Fragment,
    modifier: Modifier = Modifier,
) {
    val containerId = rememberSaveable { View.generateViewId() }

    val localView = LocalView.current
    // Find the parent fragment, if one exists. This will let us ensure that fragments inflated via a
    // FragmentContainerView are properly nested (which, in turn, allows the fragments to properly save/restore their
    // state)
    val parentFragment = remember(localView) {
        @Suppress("SwallowedException")
        try {
            localView.findFragment<Fragment>()
        } catch (e: IllegalStateException) {
            // findFragment throws if no parent fragment is found
            null
        }
    }
    val fragmentManager = parentFragment?.childFragmentManager
        ?: (LocalActivity.current as FragmentActivity).supportFragmentManager

    AndroidView(
        factory = { context ->
            val fragmentContainerView = FragmentContainerView(context).apply { id = containerId }

            // This will assign existing fragment(s) to the container instance. Must be called manually because we
            // create FragmentContainerView programmatically instead of inflating it from XML.
            fragmentManager.onContainerAvailable(fragmentContainerView)

            // If the container is empty, create the fragment and add it via FragmentManager.
            if (fragmentContainerView.getFragment<Fragment?>() == null) {
                fragmentManager.commit {
                    add(containerId, fragmentFactory())
                }
            }

            fragmentContainerView
        },
        onRelease = { fragmentContainerView ->
            val existingFragment = fragmentManager.findFragmentById(fragmentContainerView.id)
            if (existingFragment != null && !existingFragment.isStateSaved) {
                // If the state isn't saved, that means that some state change has removed this Composable from the
                // hierarchy.
                fragmentManager.commitNow {
                    remove(existingFragment)
                }
            }
        },
        modifier = modifier,
    )
}

// Access to package-private method in FragmentManager through reflection
private fun FragmentManager.onContainerAvailable(view: FragmentContainerView) {
    val method = FragmentManager::class.java.getDeclaredMethod(
        "onContainerAvailable",
        FragmentContainerView::class.java,
    )
    method.isAccessible = true
    method.invoke(this, view)
}
