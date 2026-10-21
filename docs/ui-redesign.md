# CampusCircle UI refresh

The UI uses the supplied Stitch screenshots as the reference: forest green actions, light neutral backgrounds, white outlined cards, compact author bylines, topic chips, and a five-destination bottom bar. The same system extends to Search, Saved, Login, and Signup.

## Existing behavior retained

- Supabase authentication and session setup, including the fixes pulled from GitHub.
- Live posts, search, upvotes/downvotes, bookmarking, reporting, and profile questions.
- Threaded replies, mentions, reply voting, timestamps, and saved navigation/scroll state.
- Profile photo and bio persistence through the existing profile repository.
- Gemini writing enhancement and moderation before posting questions or replies.

## Requested screen behavior

- Profile shows Saved Questions, My Questions, and Answers as inline tabs. Switching tabs keeps the user on Profile; the Answers tab reads their actual replies and parent questions from Supabase.
- Home has topic/trending filters and no search bar. Search remains in the Topics destination.
- Login and signup use the same campus photo, light outlined form cards, and forest-green controls as onboarding. Existing authentication and the pulled NMIMS signup email rule are retained.
- Question answers have Top/Newest sorting. Top orders by likes; Newest orders by creation instant. Each thread stays together and drafts survive changing the sort.
- The question form has two posting identity cards. The answer composer shows the signed-in posting identity and has no anonymous switch.

The original voting placeholder and sample profile posts have already been replaced by the pulled code. This refresh uses those implementations. Feed failures now have a retry state; profile fetch failures propagate instead of being silently converted to an empty list. Bookmark and reply failures display feedback.

## Design adaptations

The screenshots contain sample university identities and features that do not exist in this app: verification badges, rankings, attachments, notification management, and AI answer summaries. These are not presented as working features. Live author names, timestamps, counts, and topics remain sourced from the existing models. The onboarding campus photo is a generated decorative image bundled in `res/drawable-nodpi/campus_community.png`.

## Preview and device checks

Compose preview functions are available in Home, Landing, Login, Signup, Profile, AddPost, and PostDetail. Compact widths, landscape, and 1.5x text examples are included. Home, Landing, Profile and AddPost previews use sample data and do not query Supabase. Authentication previews defer repository creation until a button is pressed.

Layouts use scrollable forms, wrapping chips and actions, minimum button heights, system/keyboard insets, and centered content capped at 520–720 dp on wider displays. Previews are developer inspection targets; they do not establish that every physical device has been verified.

Verification uses the connected OnePlus Nord CE running Android 13; no emulator was started. The APK was installed as an update without clearing app data. Build, 15 JVM unit tests, and lint pass (0 errors; warnings remain). The complete device suite reports 7 passed checks and 1 skipped optional live-post check. Tests exercise reply scrolling, timestamps, mentions, drafts, Top/Newest ordering, inline profile tabs, and bookmark/navigation actions at 320 dp with 1.5x text. The optional live-post check requires a unique post title. The app's live feed, profile photo, inline saved questions, and actual question/answer counts were also observed loading from the existing Supabase session.

For further phone checks, use `app/build/outputs/apk/debug/app-debug.apk` and verify sign-in, question creation, Gemini enhancement/moderation, both vote directions, bookmarks, threaded answers, profile photo/bio updates, landscape, and the open keyboard. These checks can write live data, so the automated UI tests use fixtures instead. Gemini and all account mutations still need an actual signed-in session for full end-to-end validation.
