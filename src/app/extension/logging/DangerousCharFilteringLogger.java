package extension.logging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Marker;
import play.Logger;

import java.util.function.Supplier;

public class DangerousCharFilteringLogger extends Logger.ALogger {

    public DangerousCharFilteringLogger(Class<?> clazz) {
        super(play.api.Logger.apply(clazz));
    }

    public DangerousCharFilteringLogger(String logName) {
        super(play.api.Logger.apply(logName));
    }

    private String filterNewLines(String msg) {
        return StringUtils.normalizeSpace(msg);
    }

    @Override
    public void trace(String message) {
        super.trace(filterNewLines(message));
    }

    @Override
    public void trace(Supplier<String> msgSupplier) {
        super.trace(filterNewLines(msgSupplier.get()));
    }

    @Override
    public void trace(Marker marker, String message) {
        super.trace(marker, filterNewLines(message));
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void trace(String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void trace(Marker marker, String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    @Override
    public void trace(String message, Throwable error) {
        super.trace(filterNewLines(message), error);
    }


    @Override
    public void trace(Marker marker, String message, Throwable error) {
        super.trace(marker, filterNewLines(message), error);
    }

    @Override
    public void debug(String message) {
        super.debug(filterNewLines(message));
    }

    @Override
    public void debug(Supplier<String> msgSupplier) {
        super.debug(filterNewLines(msgSupplier.get()));
    }

    @Override
    public void debug(Marker marker, String message) {
        super.debug(marker, filterNewLines(message));
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void debug(String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void debug(String message, Supplier<?>... args) {
        throw new ForbiddenLoggingMethod();
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void debug(Marker marker, String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    @Override
    public void debug(String message, Throwable error) {
        super.debug(filterNewLines(message), error);
    }

    @Override
    public void debug(Marker marker, String message, Throwable error) {
        super.debug(marker, filterNewLines(message), error);
    }

    @Override
    public void info(String message) {
        super.info(filterNewLines(message));
    }

    @Override
    public void info(Supplier<String> msgSupplier) {
        super.info(filterNewLines(msgSupplier.get()));
    }

    @Override
    public void info(Marker marker, String message) {
        super.info(marker, filterNewLines(message));
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void info(String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void info(String message, Supplier<?>... args) {
        throw new ForbiddenLoggingMethod();
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void info(Marker marker, String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    @Override
    public void info(String message, Throwable error) {
        super.info(filterNewLines(message), error);
    }

    @Override
    public void info(Marker marker, String message, Throwable error) {
        super.info(marker, filterNewLines(message), error);
    }

    @Override
    public void warn(String message) {
        super.warn(filterNewLines(message));
    }

    @Override
    public void warn(Supplier<String> msgSupplier) {
        super.warn(filterNewLines(msgSupplier.get()));
    }

    @Override
    public void warn(Marker marker, String message) {
        super.warn(marker, filterNewLines(message));
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void warn(String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void warn(String message, Supplier<?>... args) {
        throw new ForbiddenLoggingMethod();
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void warn(Marker marker, String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    @Override
    public void warn(String message, Throwable error) {
        super.warn(filterNewLines(message), error);
    }

    @Override
    public void warn(Marker marker, String message, Throwable error) {
        super.warn(marker, filterNewLines(message), error);
    }

    @Override
    public void error(String message) {
        super.error(filterNewLines(message));
    }

    @Override
    public void error(Supplier<String> msgSupplier) {
        super.error(filterNewLines(msgSupplier.get()));
    }

    @Override
    public void error(Marker marker, String message) {
        super.error(marker, filterNewLines(message));
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void error(String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    /**
     * @deprecated
     * Use of the formatting varargs API from the underlying logging framework slf4j is forbidden.
     * We cannot filter newlines from that.
     */
    @Deprecated
    @Override
    public void error(String message, Supplier<?>... args) {
        throw new ForbiddenLoggingMethod();
    }

    @Override
    public void error(Marker marker, String message, Object... args) {
        throw new ForbiddenLoggingMethod();
    }

    @Override
    public void error(String message, Throwable error) {
        super.error(filterNewLines(message), error);
    }

    @Override
    public void error(Marker marker, String message, Throwable error) {
        super.error(marker, filterNewLines(message), error);
    }
}
