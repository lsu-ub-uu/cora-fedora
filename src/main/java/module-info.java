/**
 * The fedora module provides interfaces and classes to use a Fedora Commons System in a Cora based
 * system.
 */
module se.uu.ub.cora.fedora {
	requires java.xml;
	requires se.uu.ub.cora.httphandler;
	requires transitive se.uu.ub.cora.data;

	exports se.uu.ub.cora.fedora.reader;
}