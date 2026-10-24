package view;

/** UI lifecycle only; allows audio, timers and preparation tasks to stop on navigation/exit. */
public interface DisposableView { void dispose(); }
