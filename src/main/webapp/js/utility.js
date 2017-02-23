// ########################## Utility Functions ##########################
function getGraphName() {
	$('#GraphName').html(getCookie('graphName'));
}

function getCookie(name) {
	var value = '; ' + document.cookie;
	var parts = value.split('; ' + name + '=');

	if (parts.length == 2) {
		return parts.pop().split(';').shift();
	}
}

function evaluateRDFLiteral(literal) {
	const splitIdx = literal.indexOf('^^');

	if (splitIdx > -1) {
		const litVal = literal.substring(0, splitIdx);
		return litVal;
	}

	return literal;
}
