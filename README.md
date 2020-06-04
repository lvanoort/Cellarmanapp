# Cellarman
Functionality-wise, Cellarman is an app for tracking the fill status and sampling results of
vessels containing aging beer.

However, it's actual purpose is to be a non-trivial (ie not a TODO app) testbed for fiddling
around with some architectural concepts. As such, the UI is rather crude, some features aren't
 really implemented and the application lacks testing.

## The Architectural Goal
While all the modules in the application are currently Android library modules, they are structured
so that, in theory, the Android dependency could be removed in order to make it possible to add
additional modules in order to make a native iOS app as well as possibly a Kotlin/JS web application.

Therefore, the application logic is fully divorced from the UI implementation, the persistence
layer, and the app compile module itself. As such, some of the custom code performs very similar
tasks to code that already exists in the Android ecosystem (SMWad for instance), but this is
necessary to keep the application from having a significant dependency on Android itself.

## Stuntman - UI "library"
The UI-side architecture is a fairly simple custom MVVM implementation, but on top of that
an additional layer of Tasks is added. A Task is meant to represent a logical operation one
might be doing, so the tasks are responsible for creating the viewmodels that are desired
and handling their outputs in order to make the application behave as desired. In other words,
a Task encapsulates several viewmodels in order to make them perform a useful task, hence the name.

Tasks are also responsible for attaching their viewmodels to the relevant views on the Canvas that
is provided to them. Due to how Android handles orientation changes/app backgrounding, the system
is structured so that Tasks are provided with Canvases to bind to, with Canvases containing the
actual views in the Android system.

Tasks can also handle process-restart persistence of state via storing and reloading data from
Wads, although the Wad implementation is currently quite primitive. The wad system is intended to
be used for handling process restarts, so its usage is slightly different than the typical Android
saveInstanceState system and wrappers around that system. Since the tasks are intended to live
at the application level, storing state across orientation changes and other non-process
restart occurrences is not necessary, so the data in the wads can be a bit briefer since they
are not used as often as instance bundles. That said, due to limitations of the system, they are
written to on orientation changes even if they are not read from after an orientation change.