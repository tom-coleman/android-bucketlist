package edu.vt.cs5254.bucketlist

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.UriMatchers.*
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.intent.IntentCallback
import androidx.test.runner.intent.IntentMonitorRegistry
import com.google.android.material.R.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val GOAL_LAST_UPDATED= "Last updated 2024-10-01 at "

@RunWith(AndroidJUnit4::class)
class P2CTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun addNewGoalAppBarMenu() {
        onView(withId(R.id.new_goal))
            .check(matches(instanceOf(ActionMenuItemView::class.java)))
            .perform(click())
        onView(withId(R.id.title_text))
            .check(matches(withText("")))
        onView(withId(R.id.completed_checkbox))
            .check(matches(not(isChecked())))
        onView(withId(R.id.paused_checkbox))
            .check(matches(not(isChecked())))
    }

    @Test
    fun addNewGoalAddProgressSetPaused() {
        onView(withId(R.id.new_goal))
            .perform(click())
        onView(withId(R.id.title_text))
            .perform(replaceText("A New Goal"))
            .perform(closeSoftKeyboard())
        addProgress("Just Starting")
        onView(withId(R.id.paused_checkbox))
            .perform(click())
        addProgress("Needed to pause")
        pressBack()
        onView(withId(R.id.goal_recycler_view))
            .check(
                matches(
                    atPosition(
                        0,
                        allOf(
                            hasDescendant(withText("A New Goal")),
                            hasDescendant(withText("Progress: 2")),
                            hasDescendant(
                                allOf(
                                    withId(R.id.list_item_image),
                                    matchesDrawable(R.drawable.ic_goal_paused),
                                    withEffectiveVisibility(Visibility.VISIBLE)
                                )
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun swipeLeftDeleteGoalTwo() {
        onView(withText("Ride in a hot air balloon"))
            .perform(swipeLeft())
        onView(withId(R.id.goal_recycler_view))
            .check(matches(atPosition(2, hasDescendant(withText("Foster or adopt a pet")))))
    }

    @Test
    fun swipeRightNotDeleteGoalFour() {
        onView(withText("Earn a graduate degree"))
            .perform(swipeRight())
        onView(withId(R.id.title_text))
            .check(matches(withText("Earn a graduate degree")))
        pressBack()
        onView(withId(R.id.goal_recycler_view))
            .check(matches(atPosition(4, hasDescendant(withText("Earn a graduate degree")))))
    }

    @Test
    fun swipeDeleteAllCheckNoGoalTextAndButton() {
        onView(withId(R.id.no_goal_text))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.no_goal_button))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.goal_recycler_view)).perform(
            repeatedlyUntil(
                actionOnItemAtPosition<GoalHolder>(0, swipeLeft()),
                hasChildCount(0),
                22
            )
        )
        onView(withId(R.id.no_goal_text))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(instanceOf(TextView::class.java)))
        onView(withId(R.id.no_goal_button))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
            .check(matches(instanceOf(Button::class.java)))
    }

    @Test
    fun swipeDeleteAllAddGoal() {
        onView(withId(R.id.goal_recycler_view)).perform(
            repeatedlyUntil(
                actionOnItemAtPosition<GoalHolder>(0, swipeLeft()),
                hasChildCount(0),
                22
            )
        )
        onView(withId(R.id.no_goal_button))
            .perform(click())
        onView(withId(R.id.title_text))
            .perform(replaceText("First Goal"))
            .perform(closeSoftKeyboard())
        pressBack()
        onView(withId(R.id.goal_recycler_view))
            .check(
                matches(
                    atPosition(
                        0,
                        allOf(
                            hasDescendant(withText("First Goal")),
                            hasDescendant(withText("Progress: 0")),
                            hasDescendant(
                                allOf(
                                    withId(R.id.list_item_image),
                                    withEffectiveVisibility(Visibility.GONE)
                                )
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun shareGoalSix() {
        Intents.init()
        onView(withText("Swim with dolphins"))
            .perform(click())
        onView(withId(R.id.share_goal_menu))
            .check(matches(instanceOf(ActionMenuItemView::class.java)))
            .perform(click())
        intended(chooser(hasAction(Intent.ACTION_SEND)))
        intended(
            chooser(
                hasExtra(
                    containsString(Intent.EXTRA_TEXT),
                    allOf(
                        startsWith("Swim with dolphins"),
                        containsString(GOAL_LAST_UPDATED),
                        containsString("Progress:"),
                        containsString(" * Step One"),
                        containsString(" * Step Two"),
                        endsWith("\nThis goal has been Completed.\n")
                    )
                )
            )
        )
        Intents.release()
    }

    @Test
    fun shareGoalEight() {
        Intents.init()
        onView(withId(R.id.goal_recycler_view))
            .perform(scrollToPosition<GoalNoteHolder>(8))
        onView(withText("Travel to every continent"))
            .perform(click())
        onView(withId(R.id.share_goal_menu))
            .check(matches(instanceOf(ActionMenuItemView::class.java)))
            .perform(click())
        intended(chooser(hasAction(Intent.ACTION_SEND)))
        intended(
            chooser(
                hasExtra(
                    containsString(Intent.EXTRA_TEXT),
                    allOf(
                        startsWith("Travel to every continent"),
                        containsString(GOAL_LAST_UPDATED),
                        not(containsString("Progress:")),
                        not(containsString(" * ")),
                        endsWith("\nThis goal has been Paused.\n")
                    )
                )
            )
        )
        Intents.release()
    }

    @Test
    fun takePhotoCheckIntent() {
        Intents.init()
        onView(withText("Earn a graduate degree"))
            .perform(click())
        onView(withId(R.id.take_photo_menu))
            .check(matches(instanceOf(ActionMenuItemView::class.java)))
            .perform(click())
        intended(hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
        intended(
            hasExtra(
                containsString("output"),
                allOf(
                    hasScheme("content"),
                    hasHost("edu.vt.cs5254.bucketlist.fileprovider"),
                    hasPath(startsWith("/goal_photos/")),
                    hasPath(endsWith("IMG_01234567-89ab-cdef-fedc-ba9876543215.JPG"))
                )
            )
        )
        Intents.release()
    }

    @Test
    fun takePhotoCheckResult() {
        Intents.init()

        val tgtContext = InstrumentationRegistry.getInstrumentation().targetContext

        IntentMonitorRegistry.getInstance().addIntentCallback(object : IntentCallback {
            override fun onIntentSent(intent: Intent?) {
                @Suppress("Deprecation")
                val uri: Uri = intent?.getParcelableExtra(MediaStore.EXTRA_OUTPUT) ?: return
                val icon = BitmapFactory.decodeResource(
                    tgtContext.resources,
                    R.drawable.ic_goal_completed
                )
                val os = tgtContext.contentResolver.openOutputStream(uri)
                os?.run {
                    icon.compress(Bitmap.CompressFormat.JPEG, 100, this)
                    flush()
                    close()
                }
            }
        })

        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(
            ActivityResult(Activity.RESULT_OK, Intent())
        )

        onView(withText("Earn a graduate degree"))
            .perform(click())
        onView(withId(R.id.goal_photo))
            .check(matches(withTagValue(nullValue())))

        onView(withId(R.id.take_photo_menu))
            .perform(click())

        onView(withId(R.id.goal_photo))
            .check(matches(withTagValue(`is`("IMG_01234567-89ab-cdef-fedc-ba9876543215.JPG"))))

        Intents.release()
    }

    @Test
    fun checkZoomDisabledWithoutPhoto() {

        val neutral50color = ContextCompat.getColor(
            InstrumentationRegistry.getInstrumentation().targetContext,
            color.material_dynamic_neutral50
        )

        onView(withText("Earn a graduate degree"))
            .perform(click())
        onView(withId(R.id.goal_photo))
            .check(matches(withTagValue(nullValue())))
            .check(matches(not(isEnabled())))
            .check(matches(withBackgroundColor(neutral50color)))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun checkZoomPhoto() {
        takePhotoCheckResult()
        onView(withId(R.id.goal_photo))
            .perform(click())
        onView(withId(R.id.image_detail))
            .inRoot(isDialog())
            .check(matches(instanceOf(ImageView::class.java)))
            .check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun checkGoalNoteRecycler() {
        onView(withText("Foster or adopt a pet"))
            .perform(click())
        onView(withId(R.id.goal_note_recycler_view))
            .check(
                matches(
                    atPosition(
                        0,
                        hasDescendant(
                            allOf(
                                instanceOf(Button::class.java),
                                not(isEnabled()),
                                withText("Step One")
                            )
                        )
                    )
                )
            )
            .check(
                matches(
                    atPosition(
                        1,
                        hasDescendant(
                            allOf(
                                instanceOf(Button::class.java),
                                not(isEnabled()),
                                withText("Step Two")
                            )
                        )
                    )
                )
            )

            .check(
                matches(
                    atPosition(
                        2,
                        hasDescendant(
                            allOf(
                                instanceOf(Button::class.java),
                                not(isEnabled()),
                                withText("Step Three")
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun scrollGoalNoteRecycler() {
        onView(withText("Foster or adopt a pet"))
            .perform(click())
        addProgress("Step Four")
        addProgress("Step Five")
        addProgress("Step Six")
        addProgress("Step Seven")
        addProgress("Step Eight")
        addProgress("Step Nine")
        addProgress("Step Ten")
        onView(withId(R.id.paused_checkbox)).perform(click())

        onView(withId(R.id.goal_note_recycler_view))
            .perform(scrollToPosition<GoalNoteHolder>(0))
            .check(
                matches(
                    atPosition(
                        3,
                        hasDescendant(
                            allOf(
                                instanceOf(Button::class.java),
                                not(isEnabled()),
                                withText("Step Four")
                            )
                        )
                    )
                )
            )
            .check(
                matches(
                    atPosition(
                        4,
                        hasDescendant(
                            allOf(
                                instanceOf(Button::class.java),
                                not(isEnabled()),
                                withText("Step Five")
                            )
                        )
                    )
                )
            )
            .check(
                matches(
                    atPosition(
                        5,
                        hasDescendant(
                            allOf(
                                instanceOf(Button::class.java),
                                not(isEnabled()),
                                withText("Step Six")
                            )
                        )
                    )
                )
            )
            .check(
                matches(
                    atPosition(
                        6,
                        hasDescendant(
                            allOf(
                                instanceOf(Button::class.java),
                                not(isEnabled()),
                                withText("Step Seven")
                            )
                        )
                    )
                )
            )
            .perform(scrollToPosition<GoalNoteHolder>(10))
            .check(
                matches(
                    atPosition(
                        9,
                        hasDescendant(
                            allOf(
                                instanceOf(Button::class.java),
                                not(isEnabled()),
                                withText("Step Ten")
                            )
                        )
                    )
                )
            )
            .check(
                matches(
                    atPosition(
                        10,
                        hasDescendant(
                            allOf(
                                instanceOf(Button::class.java),
                                not(isEnabled()),
                                withText("PAUSED")
                            )
                        )
                    )
                )
            )

    }

    @Test
    fun swipeDeleteProgressNotes() {
        onView(withText("Foster or adopt a pet"))
            .perform(click())
        onView(withId(R.id.goal_note_recycler_view))
            .check(matches(hasChildCount(3)))
        onView(withText("Step One"))
            .perform(swipeLeft())
        onView(withText("Step Two"))
            .perform(swipeLeft())
        onView(withText("Step Three"))
            .perform(swipeLeft())
        onView(withId(R.id.goal_note_recycler_view))
            .check(matches(hasChildCount(0)))
        pressBack()
        onView(withId(R.id.goal_recycler_view))
            .check(matches(atPosition(0, hasDescendant(withText("Progress: 0")))))
    }

    @Test
    fun swipeNonProgressNotesAndSwipeRightProgress() {
        onView(withText("Ride in a hot air balloon"))
            .perform(click())
        onView(withId(R.id.goal_note_recycler_view))
            .check(matches(hasChildCount(3)))
        onView(withText("PAUSED"))
            .perform(swipeLeft())
        onView(withText("Step One"))
            .perform(swipeRight())
        onView(withText("Step Two"))
            .perform(swipeRight())
        onView(withId(R.id.goal_note_recycler_view))
            .check(matches(hasChildCount(3)))
    }


    //  ------ PRIVATE HELPER METHODS BELOW HERE ------

    private fun withBackgroundColor(@ColorRes color: Int): BoundedMatcher<View, ImageView> {
        return object : BoundedMatcher<View, ImageView>(ImageView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("image view with background color")
            }

            override fun matchesSafely(item: ImageView): Boolean {
                val colorDrawable = item.background as ColorDrawable
                return colorDrawable.color == color
            }
        }
    }

    private fun matchesDrawable(resourceID: Int): Matcher<View?> {
        return object : BoundedMatcher<View?, ImageView>(ImageView::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("an ImageView with resourceID: ")
                description.appendValue(resourceID)
            }

            override fun matchesSafely(imageView: ImageView): Boolean {
                val expBM = imageView.context.resources
                    .getDrawable(resourceID, null).toBitmap()
                return imageView.drawable?.toBitmap()?.sameAs(expBM) ?: false
            }
        }
    }

    private fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?> {
        return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {

            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position) ?: return false
                return itemMatcher.matches(viewHolder.itemView)
            }
        }
    }

    private fun addProgress(progressText: String) {
        onView(withId(R.id.add_progress_button))
            .perform(click())
        onView(withId(R.id.progress_text))
            .perform(replaceText(progressText))
        onView(withText("Add"))
            .inRoot(isDialog())
            .perform(click())
    }

    private fun chooser(matcher: Matcher<Intent>): Matcher<Intent> {
        return allOf(
            hasAction(Intent.ACTION_CHOOSER),
            hasExtra(`is`(Intent.EXTRA_INTENT), matcher)
        )
    }

}
 
