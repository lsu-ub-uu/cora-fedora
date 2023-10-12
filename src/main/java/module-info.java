/**
 * The fedora module provides interfaces and classes to use a Fedora Commons System in a Cora based
 * system.
 */
module se.uu.ub.cora.fedora {
	requires se.uu.ub.cora.httphandler;
	requires se.uu.ub.cora.json;

	exports se.uu.ub.cora.fedora;
	exports se.uu.ub.cora.fedora.record;
}